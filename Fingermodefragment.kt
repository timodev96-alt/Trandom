package com.example.trandom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Just hosts your existing FingerRandomizerView full-screen — none of that
 * view's logic changes, it's only being wrapped so it can live inside the
 * bottom-nav / fragment setup alongside the new spin wheel mode.
 */
class FingerModeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FingerRandomizerView(requireContext())
    }
}