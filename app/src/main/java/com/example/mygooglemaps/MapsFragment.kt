package com.example.mygooglemaps

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class MapsFragment : Fragment() {
    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null

    // Точка входа в Places API.
    private lateinit var placesClient: PlacesClient

    // Точка входа в FusedLocationProvider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Местоположение по умолчанию (Санкт-Петербург, Россия) и масштаб по умолчанию для использования при разрешении местоположения
    // не предоставлено.
    private val defaultLocation = LatLng(59.945933, 30.320045)
    private var locationPermissionGranted = false

    // Географическое положение, в котором в данный момент находится устройство. То есть последний известный
    // местоположение, полученное провайдером объединенных местоположений.
    private var lastKnownLocation: Location? = null
    private var likelyPlaceNames: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAddresses: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAttributions: Array<List<*>?> = arrayOfNulls(0)
    private var likelyPlaceLatLngs: Array<LatLng?> = arrayOfNulls(0)

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        // Используйте пользовательский адаптер информационного окна для обработки нескольких строк текста в
        // содержимое информационного окна.
        map?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(p0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View? {
                // Расширить макеты для информационного окна, заголовка и фрагмента.
                val infoWindow = layoutInflater.inflate(
                    R.layout.custom_info_contents,
                    view?.findViewById<FrameLayout>(R.id.map),
                    false
                )
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker.title
                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
                snippet.text = marker.snippet
                return infoWindow
            }
        })
        // Prompt the user for permission.
        getLocationPermission()
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()
        // Get the current location of the device and set the position of the map.
        getDeviceLocation()
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
        // Construct a PlacesClient
        Places.initialize(requireActivity(), BuildConfig.MAPS_API_KEY)
        placesClient = Places.createClient(requireActivity())
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        map?.let {
            outState.putParcelable(KEY_CAMERA_POSITION, map?.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.option_get_place) {
            showCurrentPlace()
        }
        return true
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
                            map?.moveCamera(
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
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    defaultLocation,
                                    DEFAULT_ZOOM.toFloat()
                                )
                            )
                            map?.uiSettings?.isMyLocationButtonEnabled = false
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
                requireActivity().applicationContext,
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
     * Предлагает пользователю выбрать текущее место из списка возможных мест и показывает
     * текущее место на карте - при условии, что пользователь предоставил разрешение на определение местоположения.
     */
    @SuppressLint("MissingPermission")
    private fun showCurrentPlace() {
        if (map == null) return
        if (locationPermissionGranted) {
            // Используйте поля для определения возвращаемых типов данных.
            val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            // Используйте конструктор для создания FindCurrentPlaceRequest.
            val request = FindCurrentPlaceRequest.newInstance(placeFields)

            // Получить вероятные места, т. е. предприятия и другие достопримечательности, которые
            // лучше всего соответствуют текущему местоположению устройства.
            val placeResult = placesClient.findCurrentPlace(request)
            placeResult.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val likelyPlaces = task.result

                    // Установить счетчик, обрабатывая случаи, когда возвращается менее 5 записей.
                    val count =
                        if (likelyPlaces != null && likelyPlaces.placeLikelihoods.size < M_MAX_ENTRIES) {
                            likelyPlaces.placeLikelihoods.size
                        } else {
                            M_MAX_ENTRIES
                        }
                    var i = 0
                    likelyPlaceNames = arrayOfNulls(count)
                    likelyPlaceAddresses = arrayOfNulls(count)
                    likelyPlaceAttributions = arrayOfNulls<List<*>?>(count)
                    likelyPlaceLatLngs = arrayOfNulls(count)
                    for (placeLikelihood in likelyPlaces?.placeLikelihoods ?: emptyList()) {
                        // Создаем список вероятных мест для показа пользователю.
                        likelyPlaceNames[i] = placeLikelihood.place.name
                        likelyPlaceAddresses[i] = placeLikelihood.place.address
                        likelyPlaceAttributions[i] = placeLikelihood.place.attributions
                        likelyPlaceLatLngs[i] = placeLikelihood.place.latLng
                        i++
                        if (i > count - 1) break
                    }
                    // Показать диалоговое окно, предлагающее пользователю список вероятных мест, и добавить
                    // маркер в выбранном месте.
                    openPlacesDialog()
                } else {
                    Log.e(TAG, "Exception: %s", task.exception)
                }
            }
        } else {
            // Пользователь не предоставил разрешения.
            Log.i(TAG, "The user did not grant location permission.")

            // Добавляем маркер по умолчанию, потому что пользователь не выбрал место.
            map?.addMarker(
                MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(defaultLocation)
                    .snippet(getString(R.string.default_info_snippet))
            )

            // Запрашиваем у пользователя разрешение.
            getLocationPermission()
        }
    }

    /**
     * Отображает форму, позволяющую пользователю выбрать место из списка возможных мест.
     */
    private fun openPlacesDialog() {
        // Попросите пользователя выбрать место, где он сейчас находится.
        val listener =
            DialogInterface.OnClickListener { dialog, which -> // Аргумент "which" содержит позицию выбранного элемента.
                val markerLatLng = likelyPlaceLatLngs[which]
                var markerSnippet = likelyPlaceAddresses[which]
                if (likelyPlaceAttributions[which] != null) {
                    markerSnippet =
                        """$markerSnippet${likelyPlaceAttributions[which]}""".trimIndent()
                }

                if (markerLatLng == null) {
                    return@OnClickListener
                }

                // Добавляем маркер для выбранного места с информационным окном
                // показ информации об этом месте.
                map?.addMarker(
                    MarkerOptions()
                        .title(likelyPlaceNames[which])
                        .position(markerLatLng)
                        .snippet(markerSnippet)
                )

                // Поместите камеру карты в положение маркера.
                map?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        markerLatLng,
                        DEFAULT_ZOOM.toFloat()
                    )
                )
            }

        // Показать диалог.
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.pick_place)
            .setItems(likelyPlaceNames, listener)
            .show()

    }

    /**
     * Обновляет настройки пользовательского интерфейса карты в зависимости от того, предоставил ли пользователь разрешение на определение местоположения.
     */
    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (map == null) return
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    companion object {
        private val TAG = MapsFragment::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

        // Используется для выбора текущего места
        private const val M_MAX_ENTRIES = 5
    }
}