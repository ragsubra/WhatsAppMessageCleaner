package com.example.wacleaner

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Fragile proof-of-concept. WhatsApp exposes no stable public UI contract.
 * This service only acts while explicitly armed and only on the active WhatsApp window.
 */
class CleanerAccessibilityService : AccessibilityService() {
    private lateinit var store: RuleStore
    private val handler = Handler(Looper.getMainLooper())
    private var busy = false
    private var lastActionAt = 0L

    override fun onServiceConnected() {
        store = RuleStore(this)
        store.setStatus("Accessibility service enabled; idle")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::store.isInitialized || !store.isArmed() || busy) return
        val packageName = event?.packageName?.toString().orEmpty()
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return
        if (System.currentTimeMillis() - lastActionAt < 900) return

        val rule = store.armedRule() ?: run {
            store.disarm(); return
        }
        val root = rootInActiveWindow ?: return
        val all = flatten(root)
        val titleMatches = all.any { nodeText(it).equals(rule.groupName, ignoreCase = true) }
        if (!titleMatches) {
            store.setStatus("Armed, but active chat title does not exactly match '${rule.groupName}'")
            recycle(all)
            return
        }

        val candidates = findMessageCandidates(all, rule.userNames)
        if (candidates.isEmpty()) {
            store.setStatus("In '${rule.groupName}': no visible messages matched ${rule.userNames.joinToString()}")
            recycle(all)
            return
        }

        store.setStatus("Found ${candidates.size} visible candidate(s) in '${rule.groupName}'")
        if (!store.deleteMode()) {
            recycle(all)
            return
        }

        // One message per event cycle for predictability. The user can stop/disarm at any time.
        val target = candidates.first()
        val clickable = nearestActionable(target, AccessibilityNodeInfo.ACTION_LONG_CLICK)
        if (clickable == null) {
            store.setStatus("Match found, but WhatsApp did not expose a long-clickable message node")
            recycle(all)
            return
        }

        busy = true
        lastActionAt = System.currentTimeMillis()
        val selected = clickable.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        if (!selected) {
            store.setStatus("Could not select matching message")
            busy = false
            recycle(all)
            return
        }

        handler.postDelayed({ clickDeleteThenDeleteForMe() }, 700)
        recycle(all)
    }

    private fun clickDeleteThenDeleteForMe() {
        val root = rootInActiveWindow
        if (root == null) { busy = false; return }
        val nodes = flatten(root)
        val delete = nodes.firstOrNull {
            val t = nodeText(it).lowercase(Locale.getDefault())
            t == "delete" || it.viewIdResourceName?.contains("delete", ignoreCase = true) == true
        }
        if (delete == null || !performClick(delete)) {
            store.setStatus("Message selected, but Delete control was not found. Tap Stop and inspect WhatsApp UI.")
            busy = false
            recycle(nodes)
            return
        }
        recycle(nodes)
        handler.postDelayed({ confirmDeleteForMe() }, 700)
    }

    private fun confirmDeleteForMe() {
        val root = rootInActiveWindow
        if (root == null) { busy = false; return }
        val nodes = flatten(root)
        val confirm = nodes.firstOrNull {
            val t = nodeText(it).lowercase(Locale.getDefault())
            t == "delete for me" || t.contains("delete for me")
        }
        if (confirm != null && performClick(confirm)) {
            store.setStatus("Deleted one matching message from your copy; continuing while armed")
            lastActionAt = System.currentTimeMillis()
        } else {
            store.setStatus("Delete dialog opened, but 'Delete for me' was not found. Locale/UI may be unsupported.")
        }
        recycle(nodes)
        busy = false
    }

    private fun findMessageCandidates(nodes: List<AccessibilityNodeInfo>, users: List<String>): List<AccessibilityNodeInfo> {
        val normalizedUsers = users.map { it.trim().lowercase(Locale.getDefault()) }
        return nodes.filter { node ->
            val combined = buildString {
                append(nodeText(node)); append(' ')
                append(node.contentDescription?.toString().orEmpty())
            }.lowercase(Locale.getDefault())
            normalizedUsers.any { user ->
                combined == user || combined.startsWith("$user:") || combined.contains("sender $user")
            }
        }.distinctBy {
            val r = Rect(); it.getBoundsInScreen(r); "${r.left}:${r.top}:${r.right}:${r.bottom}"
        }
    }

    private fun nodeText(node: AccessibilityNodeInfo): String =
        node.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
            ?: node.contentDescription?.toString()?.trim().orEmpty()

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        val clickable = nearestActionable(node, AccessibilityNodeInfo.ACTION_CLICK)
        return clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    private fun nearestActionable(start: AccessibilityNodeInfo, action: Int): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = start
        repeat(6) {
            if (node?.actionList?.any { it.id == action } == true) return node
            node = node?.parent
        }
        return null
    }

    private fun flatten(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val out = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && out.size < 2500) {
            val n = queue.removeFirst()
            out.add(n)
            for (i in 0 until n.childCount) n.getChild(i)?.let(queue::addLast)
        }
        return out
    }

    private fun recycle(nodes: List<AccessibilityNodeInfo>) {
        // recycle() is deprecated on newer Android versions; framework manages lifecycle.
    }

    override fun onInterrupt() {
        if (::store.isInitialized) store.setStatus("Accessibility service interrupted")
        busy = false
    }
}
