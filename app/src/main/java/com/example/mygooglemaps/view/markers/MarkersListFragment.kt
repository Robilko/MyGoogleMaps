package com.example.mygooglemaps.view.markers

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.mygooglemaps.R
import com.example.mygooglemaps.utils.Utils.Companion.TAG_EDIT_MARKER_DIALOG_FRAGMENT
import com.example.mygooglemaps.databinding.FragmentMarkersListBinding
import com.example.mygooglemaps.viewmodel.SharedViewModel
import com.google.android.gms.maps.model.Marker

class MarkersListFragment : Fragment() {

    private var _binding: FragmentMarkersListBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedViewModel

    private val recyclerItemListener = object : RecyclerItemListener {
        override fun onItemClick(marker: Marker) {
            showEditMarkerDialogFragment(marker.id)
        }

        override fun onItemLongClick(marker: Marker) {
            showDeleteDialog(marker)
        }
    }

    private val markersListAdapter: MarkersListAdapter =
        MarkersListAdapter(listener = recyclerItemListener)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkersListBinding.inflate(inflater, container, false)
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

    private fun initView() = with(binding) {
        mainRecycler.adapter = markersListAdapter
        val markers = sharedViewModel.getAllMarkers()
        if (markers.isNullOrEmpty()) {
            emptyListImage.visibility = View.VISIBLE
        } else {
            markersListAdapter.submitList(markers)
        }
        sharedViewModel.markersLiveData.observe(requireActivity()) {
            markersListAdapter.submitList(
                it
            )
        }
    }

    private fun initViewModel() {
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
    }

    private fun showEditMarkerDialogFragment(markerId: String) {
        MarkerDialogFragment.newInstance(markerId)
            .show(parentFragmentManager, TAG_EDIT_MARKER_DIALOG_FRAGMENT)
    }

    private fun showDeleteDialog(marker: Marker) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_warning)
            .setPositiveButton(R.string.yes_button) { _, _ -> sharedViewModel.deleteMarker(marker) }
            .setNegativeButton(R.string.no_button) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    companion object {
        fun newInstance() = MarkersListFragment()
    }
}