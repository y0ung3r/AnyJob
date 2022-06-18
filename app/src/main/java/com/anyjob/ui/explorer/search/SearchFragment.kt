package com.anyjob.ui.explorer.search

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.anyjob.R
import com.anyjob.databinding.FragmentSearchBinding
import com.anyjob.domain.profile.models.MapsAddress
import com.anyjob.domain.profile.models.User
import com.anyjob.domain.search.OrderCreationParameters
import com.anyjob.ui.animations.VisibilityMode
import com.anyjob.ui.animations.extensions.fade
import com.anyjob.ui.animations.fade.FadeParameters
import com.anyjob.ui.animations.radar.RadarParameters
import com.anyjob.ui.animations.radar.extensions.startRadar
import com.anyjob.ui.explorer.ExplorerActivity
import com.anyjob.ui.explorer.search.controls.bottomSheets.GeolocationUnavailableBottomSheetDialog
import com.anyjob.ui.explorer.search.controls.bottomSheets.addresses.AddressesBottomSheetDialog
import com.anyjob.ui.explorer.search.controls.bottomSheets.addresses.models.UserAddress
import com.anyjob.ui.explorer.search.viewModels.SearchViewModel
import com.anyjob.ui.explorer.viewModels.ExplorerViewModel
import com.anyjob.ui.extensions.getZoomLevel
import com.anyjob.ui.extensions.observeOnce
import com.anyjob.ui.extensions.showToast
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class SearchFragment : Fragment() {
    private val _activityViewModel by sharedViewModel<ExplorerViewModel>()
    private val _viewModel by viewModel<SearchViewModel>()
    private lateinit var _binding: FragmentSearchBinding
    private lateinit var _googleMap: GoogleMap
    private lateinit var _addressesBottomSheet: BottomSheetDialog
    private var _searchRadiiViews = ArrayList<Circle>()

    private val _toolbar by lazy {
        val activity = requireActivity() as ExplorerActivity
        return@lazy activity.binding.toolbar
    }

    private val _mapView by lazy {
        childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    }

    private val _searchBottomSheetBehavior by lazy {
        BottomSheetBehavior.from(_binding.searchBottomSheet.bottomSheetLayout)
    }

    private val _searchProgressBottomSheetBehavior by lazy {
        BottomSheetBehavior.from(_binding.searchProgressBottomSheet.bottomSheetLayout)
    }

    private fun getSearchRadius(chipId: Int): Float = when (chipId) {
        R.id.one_kilometer_chip -> 1000.0f
        R.id.two_kilometers_chip -> 2000.0f
        R.id.three_kilometers_chip -> 3000.0f
        R.id.five_kilometers_chip -> 5000.0f
        else -> 500.0f
    }

    private val _searchBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (bottomSheet.visibility == View.VISIBLE) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> drawSearchRadius(
                        _googleMap.cameraPosition.target,
                        getSearchRadius(_binding.searchBottomSheet.availableRadii.checkedChipId)
                    )

                    BottomSheetBehavior.STATE_COLLAPSED -> removeLastSearchRadius()
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            // Ignore...
        }
    }

    private val _locationProvider by lazy {
        LocationServices.getFusedLocationProviderClient(
            requireActivity()
        )
    }

    private val _locationPermissions = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            showLocationPermissionsRationaleDialog()
        }
    }

    private fun requestLocationPermissions() {
        _locationPermissions.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun showLocationPermissionsRationaleDialog() {
        val context = requireContext()

        GeolocationUnavailableBottomSheetDialog(
            context,
            R.style.Theme_AnyJob_BottomSheetDialog
        )
        .show()
    }

    private fun isPermissionsDenied(): Boolean {
        val context = requireContext()

        val finePermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarsePermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return !finePermissionGranted || !coarsePermissionGranted
    }

    private fun moveCamera(address: Address) {
        val radius = getSearchRadius(
            _binding.searchBottomSheet.availableRadii.checkedChipId
        )

        moveCamera(
            LatLng(address.latitude, address.longitude),
            getZoomLevel(radius)
        )
    }

    private fun moveCamera(location: LatLng, zoom: Float) {
        val coordinates = LatLng(location.latitude, location.longitude)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(coordinates, zoom)
        _googleMap.animateCamera(cameraUpdate)
    }

    private fun moveCameraToUserLocation() {
        if (isPermissionsDenied()) {
            return requestLocationPermissions()
        }

        if (!_googleMap.isMyLocationEnabled) {
            _googleMap.isMyLocationEnabled = true
        }

        _locationProvider.lastLocation.addOnSuccessListener { location ->
            val radius = getSearchRadius(
                _binding.searchBottomSheet.availableRadii.checkedChipId
            )

            moveCamera(
                LatLng(location.latitude, location.longitude),
                getZoomLevel(radius)
            )
        }
    }

    private fun drawSearchRadius(position: LatLng, radius: Float, radarParameters: RadarParameters? = null): Circle {
        removeLastSearchRadius()

        val searchRadius = _googleMap.addCircle(
            CircleOptions().apply {
                center(position)
                strokeColor(Color.TRANSPARENT)
                fillColor(
                    Color.parseColor(
                        getString(R.color.light_purple)
                    )
                )
            }
        )

        val animationParameters = radarParameters ?: RadarParameters()
        val fillColor = Color.alpha(searchRadius.fillColor)

        startRadar(
            animationParameters.apply {
                mode = VisibilityMode.Show
                maxRadius = radius
                onUpdate = { radiusFraction, invertedRadiusFraction ->
                    searchRadius.radius = radiusFraction

                    if (infinity) {
                        val alpha = (invertedRadiusFraction / radius * fillColor).toInt()
                        searchRadius.fillColor = ColorUtils.setAlphaComponent(searchRadius.fillColor, alpha)
                    }
                }
            }
        )

        _searchRadiiViews.add(searchRadius)

        return searchRadius
    }

    private fun removeLastSearchRadius() {
        _searchRadiiViews.lastOrNull()?.also {
            startRadar(
                RadarParameters().apply {
                    mode = VisibilityMode.Hide
                    animationLength = 500
                    maxRadius = it.radius.toFloat()
                    onUpdate = { radiusFraction, _ ->
                        it.radius = radiusFraction

                        if (it.radius == 0.0) {
                            it.remove()
                            _searchRadiiViews.remove(it)
                        }
                    }
                }
            )
        }
    }

    private fun onMapReady(googleMap: GoogleMap) {
        _googleMap = googleMap

        _googleMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )

        val geocoder = Geocoder(
            requireContext(),
            Locale.getDefault()
        )

        _googleMap.uiSettings.isMyLocationButtonEnabled = true
        _googleMap.uiSettings.isRotateGesturesEnabled = false
        _googleMap.uiSettings.isTiltGesturesEnabled = false
        _googleMap.uiSettings.isMyLocationButtonEnabled = false

        _googleMap.setOnCameraIdleListener {
            lifecycleScope.launch {
                val position = _googleMap.cameraPosition.target

                try {
                    val addresses = withContext(Dispatchers.Default) {
                        geocoder.getFromLocation(position.latitude, position.longitude, 1)
                    }

                    if (addresses.any()) {
                        val address = addresses[0]
                        _activityViewModel.updateCurrentAddress(address)
                    }
                }

                catch (exception: Exception) {
                    showToast(
                        getString(R.string.failed_to_determine_address)
                    )
                }

                finally {
                    _searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        _googleMap.setOnCameraMoveStartedListener {
            _searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        moveCameraToUserLocation()
    }

    private fun onCurrentLocationButtonClick(button: View) {
        moveCameraToUserLocation()
    }

    private fun onUserChangeRadius(chipGroup: View, selectedChip: Int) {
        val radius = getSearchRadius(selectedChip)
        val location = _googleMap.cameraPosition.target

        drawSearchRadius(location, radius)
        moveCamera(
            LatLng(location.latitude, location.longitude),
            getZoomLevel(radius)
        )
    }

    private fun onUserStartSearching(button: View) {
        _binding.currentLocationButton.fade(
            FadeParameters().apply {
                mode = VisibilityMode.Hide
                animationLength = 300
            }
        )

        _binding.searchBottomSheet.bottomSheetLayout.visibility = View.GONE
        _binding.searchProgressBottomSheet.bottomSheetLayout.visibility = View.VISIBLE

        _binding.mapPin.isTouchEventsDisabled = true
        _googleMap.uiSettings.isScrollGesturesEnabled = false
        _googleMap.uiSettings.isZoomGesturesEnabled = false

        val chipId = _binding.searchBottomSheet.availableRadii.checkedChipId
        val chip = _binding.searchBottomSheet.availableRadii.findViewById<Chip>(chipId)
        val radius = getSearchRadius(chipId)

        val position = _googleMap.cameraPosition.target
        val radarParameters = RadarParameters().apply {
            infinity = true
            animationLength = 3500
        }

        drawSearchRadius(position, radius, radarParameters)

        moveCamera(
            position,
            getZoomLevel(radius)
        )

        _binding.searchProgressBottomSheet.searchInProgressDescription.text = getString(
            R.string.search_progress_description,
            chip.text
        )

        _activityViewModel.getAuthorizedUser().observeOnce(this@SearchFragment) { authorizedUser ->
            authorizedUser?.also {
                _viewModel.startWorkerSearching(
                    OrderCreationParameters(
                        invokerId = it.id,
                        address = MapsAddress(position.latitude, position.longitude),
                        radius.toDouble()
                    ),
                    ::onWorkerFound
                )
            }
        }
    }

    private fun onUserCancelSearching(button: View) {
        _binding.currentLocationButton.fade(
            FadeParameters().apply {
                mode = VisibilityMode.Show
                animationLength = 300
            }
        )

        _binding.searchProgressBottomSheet.bottomSheetLayout.visibility = View.GONE
        _binding.searchBottomSheet.bottomSheetLayout.visibility = View.VISIBLE

        _searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        _binding.mapPin.isTouchEventsDisabled = false
        _googleMap.uiSettings.isScrollGesturesEnabled = true
        _googleMap.uiSettings.isZoomGesturesEnabled = true

        removeLastSearchRadius()

        val position = _googleMap.cameraPosition.target
        val radius = getSearchRadius(
            _binding.searchBottomSheet.availableRadii.checkedChipId
        )

        moveCamera(
            position,
            getZoomLevel(radius)
        )

        _viewModel.cancelWorkerSearching()
    }

    private fun onWorkerFound(worker: User) {

    }

    private fun onAddressSelected(userAddress: UserAddress) {
        _addressesBottomSheet.dismiss()
        moveCamera(userAddress.source)
    }

    private fun onAddressTitleClick(view: View) {
        _addressesBottomSheet = AddressesBottomSheetDialog(
            requireContext(),
            R.style.Theme_AnyJob_BottomSheetDialog,
            ::onAddressSelected
        )

        _addressesBottomSheet.show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        _mapView.getMapAsync(::onMapReady)
        _binding.currentLocationButton.setOnClickListener(::onCurrentLocationButtonClick)

        _searchBottomSheetBehavior.apply {
            addBottomSheetCallback(_searchBottomSheetCallback)
            isGestureInsetBottomIgnored = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        _searchProgressBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        _binding.searchBottomSheet.availableRadii.setOnCheckedChangeListener(::onUserChangeRadius)
        _binding.searchBottomSheet.startSearchingButton.setOnClickListener(::onUserStartSearching)
        _binding.searchProgressBottomSheet.cancelButton.setOnClickListener(::onUserCancelSearching)
        _toolbar.setOnClickListener(::onAddressTitleClick)

        return _binding.root
    }
}