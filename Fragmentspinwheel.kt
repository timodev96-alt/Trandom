<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#121212"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <EditText
            android:id="@+id/optionInput"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="Add an option..."
            android:textColorHint="#888888"
            android:textColor="#FFFFFF"
            android:inputType="text"
            android:imeOptions="actionDone" />

        <Button
            android:id="@+id/addButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Add" />
    </LinearLayout>

    <HorizontalScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp">

        <LinearLayout
            android:id="@+id/chipContainer"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal" />
    </HorizontalScrollView>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Tap a name below to remove it"
        android:textColor="#888888"
        android:textSize="12sp"
        android:layout_marginTop="4dp" />

    <com.example.trandom.SpinWheelView
        android:id="@+id/spinWheelView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="16dp" />

    <TextView
        android:id="@+id/resultText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text=""
        android:textColor="#FFFFFF"
        android:textSize="24sp"
        android:textStyle="bold"
        android:gravity="center"
        android:layout_marginTop="8dp" />

    <Button
        android:id="@+id/spinButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="SPIN"
        android:layout_marginTop="8dp" />

</LinearLayout>