package com.example.mygooglemaps

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.mygooglemaps.Utils.Companion.TAG_EDIT_MARKER_DIALOG_FRAGMENT
import com.example.mygooglemaps.databinding.FragmentMarkersListBinding
import com.google.android.gms.maps.model.Marker

class MarkersListFragment : Fragment() {

    private var _binding: FragmentMarkersListBinding? = null
    private val binding get() = _binding!!

    private lateinit var model: SharedViewModel

    private val recyclerItemListener = object : RecyclerItemListener {
        override fun onItemClick(marker: Marker) {
            showEditMarkerDialogFragment(marker.id)
        }
    }

    private val markersListAdapter: MarkersListAdapter = MarkersListAdapter(listener = recyclerItemListener)

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
        val markers = model.getAllMarkers()
        if (markers.isNullOrEmpty()) {
            emptyListImage.visibility = View.VISIBLE
        } else {
            model.markers.observe(requireActivity()) { markersListAdapter.submitList(it) }
        }
    }

    private fun initViewModel() {
        model = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
    }

    private fun showEditMarkerDialogFragment(markerId: String) {
        MarkerDialogFragment.newInstance(markerId).show(parentFragmentManager, TAG_EDIT_MARKER_DIALOG_FRAGMENT)
    }

    companion object {
        fun newInstance() = MarkersListFragment()
    }
}