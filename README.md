<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="WhatsApp Message Cleaner — Proof of Concept"
            android:textSize="22sp"
            android:textStyle="bold" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Add one or more rules. Preview mode only reports matches. Delete mode must be armed deliberately and affects only your copy of the chat. WhatsApp UI changes may break matching."
            android:textSize="14sp" />

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:hint="Group name">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/groupInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text" />
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:hint="User names (comma-separated)">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/usersInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text" />
        </com.google.android.material.textfield.TextInputLayout>

        <Button
            android:id="@+id/addRuleButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="Add rule" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:text="Saved rules"
            android:textSize="18sp"
            android:textStyle="bold" />

        <LinearLayout
            android:id="@+id/rulesContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical" />

        <Button
            android:id="@+id/accessibilityButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:text="Enable accessibility service" />

        <com.google.android.material.switchmaterial.SwitchMaterial
            android:id="@+id/deleteModeSwitch"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="14dp"
            android:text="Delete mode (otherwise preview only)" />

        <Button
            android:id="@+id/armButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Arm selected rule and open WhatsApp" />

        <Button
            android:id="@+id/disarmButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Stop / disarm" />

        <TextView
            android:id="@+id/statusText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:padding="12dp"
            android:text="Status: idle"
            android:textIsSelectable="true" />

    </LinearLayout>
</ScrollView>
