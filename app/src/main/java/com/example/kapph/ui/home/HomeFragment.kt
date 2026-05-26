package com.example.kapph.ui.home

import android.content.Intent // Added for navigation
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.widget.SearchView // Import for SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Use activityViewModels for potentially shared ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kapph.MainActivity
import com.example.kapph.ProductAdapter
import com.example.kapph.ProductDetailActivity // Added for navigation
import com.example.kapph.ProductViewModel
import com.example.kapph.R
import com.example.kapph.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Using activityViewModels as ProductViewModel might be needed by FavoritesFragment too
    private val productViewModel: ProductViewModel by activityViewModels()
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDrawerButton()
        setupSpinner(view) // Passed view to findViewById for spinner as it was before
        setupRecyclerView()
        setupSearchView()
        observeViewModel()
    }

    private fun setupDrawerButton() {
        binding.drawerButton.setOnClickListener {
            (activity as? MainActivity)?.let {
                if (it.drawerLayout.isDrawerOpen(it.navDrawer)) {
                    it.drawerLayout.closeDrawer(it.navDrawer)
                } else {
                    it.drawerLayout.openDrawer(it.navDrawer)
                }
            }
        }
    }

    private fun setupSpinner(view: View) { // Original spinner setup
        val spinner: Spinner = view.findViewById(R.id.spinnerStoreLocations)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.spinnerStoreLocationsArray,
            R.layout.spinner_list
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list)
            spinner.adapter = adapter
            spinner.dropDownVerticalOffset = 150
        }

        val arrowDownLocation = view.findViewById<ImageView>(R.id.arrowDownLocation)
        arrowDownLocation.setOnClickListener {
            spinner.performClick()
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            // Handle item click: Navigate to ProductDetailActivity
            val intent = Intent(requireActivity(), ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_NAME", product.name)
                putExtra("PRODUCT_PRICE", product.price)
                putExtra("PRODUCT_IMAGE", product.imageResId)
                putExtra("PRODUCT_DESCRIPTION", product.description)
            }
            startActivity(intent)
        }

        binding.productList.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
            // setHasFixedSize(true) // Consider removing if item content can change size,
            // or if using ListAdapter with dynamic content.
            // Usually fine for product lists.
        }
    }

    private fun setupSearchView() {
        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                productViewModel.searchProducts(query)
                binding.searchBar.clearFocus() // Hide keyboard
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                productViewModel.searchProducts(newText)
                return true
            }
        })

        // Handle click of the 'X' (clear) button in SearchView
        val closeButton = binding.searchBar.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton?.setOnClickListener {
            binding.searchBar.setQuery("", false) // Clear the text
            binding.searchBar.clearFocus()        // Remove focus, hide keyboard
            // productViewModel.searchProducts("") // searchProducts will be called by onQueryTextChange with empty string
        }
    }

    private fun observeViewModel() {
        productViewModel.displayedProducts.observe(viewLifecycleOwner) { products ->
            Log.d("HomeFragment", "Updating adapter with ${products.size} products.")
            productAdapter.submitList(products) // Use submitList for ListAdapter

            // Handle visibility based on whether the *filtered* list is empty,
            // but only show "no results" if there was an actual search query.
            val currentQuery = binding.searchBar.query.toString()
            if (products.isEmpty() && currentQuery.isNotBlank()) {
                binding.productList.isVisible = false
                binding.textViewNoResults.isVisible = true
            } else {
                binding.productList.isVisible = true
                binding.textViewNoResults.isVisible = false
            }
        }

        // This LiveData from ViewModel is more direct for "no search results" state
        // Let's use it instead of the logic in displayedProducts observer for clarity
        productViewModel.noSearchResults.observe(viewLifecycleOwner) { noResultsFound ->
            val currentQuery = binding.searchBar.query.toString()
            if (noResultsFound && currentQuery.isNotBlank()) { // Only show "no results" if there was a query
                binding.productList.isVisible = false
                binding.textViewNoResults.isVisible = true
            } else if (productViewModel.displayedProducts.value.isNullOrEmpty() && currentQuery.isNotBlank()){
                // Handles case where displayedProducts is empty due to search, but noSearchResults might not have updated yet
                binding.productList.isVisible = false
                binding.textViewNoResults.isVisible = true
            }
            else {
                // If not "no results" OR query is blank (meaning show all)
                binding.productList.isVisible = true
                binding.textViewNoResults.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the query in SearchView when view is destroyed to prevent it from persisting
        // if the fragment is put on backstack and restored.
        binding.searchBar.setQuery("", false)
        _binding = null
    }
}