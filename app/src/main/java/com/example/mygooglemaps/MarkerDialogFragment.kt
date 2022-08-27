package com.example.mygooglemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.example.mygooglemaps.Utils.Companion.TAG_EDIT_MARKER_DIALOG_FRAGMENT
import com.example.mygooglemaps.Utils.Companion.TAG_MARKERS_LIST_FRAGMENT
import com.example.mygooglemaps.databinding.FragmentMarkerDialogBinding
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MarkerDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentMarkerDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedViewModel

    private val markerId: String by lazy { requireArguments().getString(ARG_MARKER_ID, "") }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkerDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initView()
    }

    private fun initViewModel() {
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
    }

    private fun initView() = with(binding) {
        val marker = getMarkerById(markerId)
        dialogFragmentMarkerTitle.setText(marker.title)
        dialogFragmentMarkerSnippet.setText(marker.snippet)
        markerSaveButton.setOnClickListener {
            marker.title = dialogFragmentMarkerTitle.text.toString()
            marker.snippet = dialogFragmentMarkerSnippet.text.toString()
            sharedViewModel.editMarker(marker)
            hideDialogFragment()
            showMarkersListFragment()
        }
    }

    private fun hideDialogFragment() {
        parentFragmentManager.findFragmentByTag(TAG_EDIT_MARKER_DIALOG_FRAGMENT)
            ?.let { parentFragmentManager.beginTransaction().remove(it).commit() }
    }

    private fun showMarkersListFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(
                R.id.main_container,
                MarkersListFragment.newInstance(),
                TAG_MARKERS_LIST_FRAGMENT
            ).commit()
    }


    private fun getMarkerById(markerId: String): Marker =
        sharedViewModel.getAllMarkers().find { it.id == markerId }!!

    companion object {
        private const val ARG_MARKER_ID = "arg_marker_id"
        fun newInstance(markerId: String) = MarkerDialogFragment().apply {
            arguments = bundleOf(ARG_MARKER_ID to markerId)
        }
    }
}