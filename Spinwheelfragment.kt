package com.example.trandom

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class SpinWheelFragment : Fragment(R.layout.fragment_spin_wheel) {

    private val options = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val input = view.findViewById<EditText>(R.id.optionInput)
        val addButton = view.findViewById<Button>(R.id.addButton)
        val chipContainer = view.findViewById<LinearLayout>(R.id.chipContainer)
        val wheel = view.findViewById<SpinWheelView>(R.id.spinWheelView)
        val resultText = view.findViewById<TextView>(R.id.resultText)
        val spinButton = view.findViewById<Button>(R.id.spinButton)

        fun refreshChips() {
            chipContainer.removeAllViews()
            options.forEachIndexed { index, name ->
                val chip = TextView(requireContext()).apply {
                    text = "$name  ✕"
                    setTextColor(0xFFFFFFFF.toInt())
                    setBackgroundColor(0xFF2A2A2A.toInt())
                    setPadding(24, 16, 24, 16)
                    textSize = 14f
                    setOnClickListener {
                        options.removeAt(index)
                        wheel.options = options.toList()
                        refreshChips()
                    }
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 12 }
                chipContainer.addView(chip, params)
            }
        }

        fun addOption() {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                options.add(text)
                wheel.options = options.toList()
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
            if (options.size < 2) {
                resultText.text = "Add at least 2 options first"
                return@setOnClickListener
            }
            resultText.text = ""
            wheel.spin { winner ->
                resultText.text = " $winner"
            }
        }
    }
}