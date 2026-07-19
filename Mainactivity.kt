package com.example.trandom

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var container: FrameLayout
    private lateinit var fingerTab: TextView
    private lateinit var wheelTab: TextView

    private val fingerView by lazy { FingerRandomizerView(this) }
    private val wheelScreen by lazy { buildWheelScreen() }

    private val wheelOptions = mutableListOf<String>()
    private lateinit var wheelView: SpinWheelView
    private lateinit var chipContainer: LinearLayout
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        container = FrameLayout(this)
        root.addView(
            container,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }

        fingerTab = makeTab("Finger Picker")
        wheelTab = makeTab("Spin Wheel")

        tabBar.addView(fingerTab, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        tabBar.addView(wheelTab, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        root.addView(
            tabBar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        )

        setContentView(root)

        fingerTab.setOnClickListener { showFinger() }
        wheelTab.setOnClickListener { showWheel() }

        showFinger()
    }

    private fun makeTab(label: String): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
            textSize = 16f
        }
    }

    private fun showFinger() {
        container.removeAllViews()
        container.addView(fingerView)
        highlightTab(fingerTab, wheelTab)
    }

    private fun showWheel() {
        container.removeAllViews()
        container.addView(wheelScreen)
        highlightTab(wheelTab, fingerTab)
    }

    private fun highlightTab(selected: TextView, other: TextView) {
        selected.setTextColor(Color.WHITE)
        selected.setBackgroundColor(Color.parseColor("#3A3A3A"))
        other.setTextColor(Color.GRAY)
        other.setBackgroundColor(Color.parseColor("#1E1E1E"))
    }

    // ---------------------------------------------------------------
    // Spin wheel screen: name input + chip list + wheel + spin button
    // ---------------------------------------------------------------

    private fun buildWheelScreen(): View {
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(16), px(16), px(16))
            setBackgroundColor(Color.parseColor("#121212"))
        }

        // --- input row ---
        val inputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val input = EditText(this).apply {
            hint = "Add an option..."
            setHintTextColor(Color.parseColor("#888888"))
            setTextColor(Color.WHITE)
        }
        inputRow.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val addButton = Button(this).apply { text = "Add" }
        inputRow.addView(addButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        screen.addView(inputRow)

        // --- chip list (tap a chip to remove it) ---
        chipContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val chipScroll = HorizontalScrollView(this).apply {
            addView(chipContainer)
        }
        val chipScrollParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        chipScrollParams.topMargin = px(8)
        screen.addView(chipScroll, chipScrollParams)

        val hint = TextView(this).apply {
            text = "Tap a name below to remove it"
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
        }
        val hintParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        hintParams.topMargin = px(4)
        screen.addView(hint, hintParams)

        // --- the wheel itself ---
        wheelView = SpinWheelView(this)
        val wheelParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        wheelParams.topMargin = px(16)
        screen.addView(wheelView, wheelParams)

        // --- result text ---
        resultText = TextView(this).apply {
            text = ""
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
        }
        val resultParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        resultParams.topMargin = px(8)
        screen.addView(resultText, resultParams)

        // --- spin button ---
        val spinButton = Button(this).apply { text = "SPIN" }
        val spinParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        spinParams.topMargin = px(8)
        screen.addView(spinButton, spinParams)

        // --- wiring ---
        fun refreshChips() {
            chipContainer.removeAllViews()
            wheelOptions.forEachIndexed { index, name ->
                val chip = TextView(this).apply {
                    text = "$name  ✕"
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#2A2A2A"))
                    setPadding(px(12), px(8), px(12), px(8))
                    textSize = 14f
                    setOnClickListener {
                        wheelOptions.removeAt(index)
                        wheelView.options = wheelOptions.toList()
                        refreshChips()
                    }
                }
                val chipParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                chipParams.marginEnd = px(8)
                chipContainer.addView(chip, chipParams)
            }
        }

        fun addOption() {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                wheelOptions.add(text)
                wheelView.options = wheelOptions.toList()
                refreshChips()
                input.text.clear()
            }
        }

        addButton.setOnClickListener { addOption() }
        input.setOnEditorActionListener { _, _, _ ->
            addOption()
            true
        }

        spinButton.setOnClickListener {
            if (wheelOptions.size < 2) {
                resultText.text = "Add at least 2 options first"
            } else {
                resultText.text = ""
                wheelView.spin { winner -> resultText.text = "🎉 $winner" }
            }
        }

        return screen
    }
}