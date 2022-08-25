package com.example.mygooglemaps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mygooglemaps.Utils.Companion.TAG_MARKERS_LIST_FRAGMENT
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsFragment : Fragment() {
    private lateinit var map: GoogleMap
    private var cameraPosition: CameraPosition? = null

    // Точка входа в FusedLocationProvider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Местоположение по умолчанию (Санкт-Петербург, Россия) и масштаб по умолчанию для использования при разрешении местоположения
    // не предоставлено.
    private val defaultLocation = LatLng(59.945933, 30.320045)
    private var locationPermissionGranted = false

    // Географическое положение, в котором в данный момент находится устройство. То есть последний известный
    // местоположение, полученное провайдером объединенных местоположений.
    private var lastKnownLocation: Location? = null

    private lateinit var model: SharedViewModel

    /**
     * Управляет картой, когда она доступна.
     * Этот обратный вызов запускается, когда карта готова к использованию.
     */
    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        // Prompt the user for permission.
        getLocationPermission()
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()
        // Get the current location of the device and set the position of the map.
        getDeviceLocation()

        map.setOnMapLongClickListener { latLng ->
            addMarkerToList(latLng)
        }

        setMarkersToMap()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)

        }
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        model = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        mapFragment?.getMapAsync(callback)
    }

    /**
     * Сохраняет состояние карты, когда активность приостановлена.
     */
    override fun onSaveInstanceState(outState: Bundle) {

        outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
        outState.putParcelable(KEY_LOCATION, lastKnownLocation)

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.current_place_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Обрабатывает щелчок по пункту меню, чтобы получить место.
     * @param item Элемент меню для обработки.
     * @return логического значения.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.option_markers_list_fragment) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container, MarkersListFragment.newInstance(),
                    TAG_MARKERS_LIST_FRAGMENT
                )
                .commit()
        }
        return true
    }

    private fun setMarkersToMap() {
        model.getAllMarkers().forEach { marker -> setMarker(marker.position, marker.title, marker.snippet) }
    }

    private fun addMarkerToList(latLng: LatLng) {
        val addressName = getAddress(latLng)
        val marker = setMarker(latLng, addressName, null)
        model.addMarkerToList(marker)
    }

    private fun setMarker(latLng: LatLng, addressName: String?, annotation: String?): Marker =
        map.addMarker(
            MarkerOptions().position(latLng).title(addressName ?: "").snippet(annotation ?: "")
        )!!

    private fun getAddress(latLng: LatLng): String {
        val geoCoder = Geocoder(requireContext())
        val addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        return addresses[0].getAddressLine(0)
    }

    /**
     * Получает текущее местоположение устройства и позиционирует камеру карты.
     */
    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Получите лучшее и самое последнее местоположение устройства, которое может быть нулевым в редких случаях
         * случаи, когда местоположение недоступно.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Установить положение камеры карты на текущее местоположение устройства.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.")
                            Log.e(TAG, "Exception: %s", task.exception)
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    defaultLocation,
                                    DEFAULT_ZOOM.toFloat()
                                )
                            )
                            map.uiSettings.isMyLocationButtonEnabled = false
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    /**
     * Запрашивает у пользователя разрешение на использование местоположения устройства.
     */
    private fun getLocationPermission() {
        /*
         * Запросите разрешение на определение местоположения, чтобы мы могли получить местоположение
         * устройство. Результат запроса разрешения обрабатывается обратным вызовом,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Обрабатывает результат запроса на разрешение местоположения.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // Если запрос отменен, массивы результатов пусты.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }

    /**
     * Обновляет настройки пользовательского интерфейса карты в зависимости от того, предоставил ли пользователь разрешение на определение местоположения.
     */
    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        try {
            if (locationPermissionGranted) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
            } else {
                map.isMyLocationEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    companion object {
        fun newInstance() = MapsFragment()
        private val TAG = MainActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }

}


