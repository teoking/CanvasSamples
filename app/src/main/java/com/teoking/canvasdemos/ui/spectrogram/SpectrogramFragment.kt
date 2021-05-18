package com.teoking.canvasdemos.ui.spectrogram

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.teoking.canvasdemos.databinding.FragmentSpectrogramBinding

class SpectrogramFragment : Fragment() {

    private lateinit var spectrogramViewModel: SpectrogramViewModel
    private var _binding: FragmentSpectrogramBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        spectrogramViewModel =
            ViewModelProvider(this).get(SpectrogramViewModel::class.java)

        _binding = FragmentSpectrogramBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.playButton.setOnClickListener { spectrogramViewModel.play() }

        val spectrogramView = binding.spectrogramView
        spectrogramViewModel.bytes.observe(viewLifecycleOwner, {
            spectrogramView.update(it)
        })

        spectrogramViewModel.fftBytes.observe(viewLifecycleOwner, {
            spectrogramView.updateFFT(it)
        })

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}