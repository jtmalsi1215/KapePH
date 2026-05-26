package com.example.kapph

import android.content.Intent // Added for navigation
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StrikethroughSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Make sure this import path is correct for your OrderSummaryAdapter
import com.example.kapph.OrderSummaryAdapter // Assuming this is where you put it
import com.example.kapph.ui.cart.CartViewModel
import java.text.NumberFormat
import java.util.Locale
// Added for OrderDetails and OrderItem
// Ensure these files are in the com.example.kapph package or update import
import com.example.kapph.OrderDetails
import com.example.kapph.OrderItem
import com.google.firebase.firestore.ktx.firestore // If not already there
import com.google.firebase.ktx.Firebase // If not already there
import com.google.firebase.auth.ktx.auth // If not already there


class OrderActivity : AppCompatActivity() {

    private val cartViewModel: CartViewModel = CartViewModel.instance

    // UI Elements
    private lateinit var toolbarTitleTextView: TextView
    private lateinit var productNameInContainerTextView: TextView
    private lateinit var productImageView: ImageView
    private lateinit var productSizeView: TextView
    private lateinit var plusButton: ImageButton
    private lateinit var minusButton: ImageButton
    private lateinit var quantityText: TextView
    private lateinit var buyNowItemsWrapper: ConstraintLayout
    private lateinit var orderSummaryRecyclerView: RecyclerView
    private lateinit var orderSummaryAdapter: OrderSummaryAdapter
    private lateinit var subtotalPriceTextView: TextView
    private lateinit var deliveryFeeTextView: TextView
    private lateinit var totalPriceTextView: TextView
    private lateinit var deliverButton: Button
    private lateinit var pickupButton: Button
    private lateinit var addressHeader: TextView
    private lateinit var editAddressButton: Button
    private lateinit var addressText: TextView
    private lateinit var placeOrderButton: Button

    private var currentSubtotal: Double = 0.0
    private var currentDeliveryFeeValue: Int = 20 // Renamed to avoid confusion with deliveryFeeTextView
    private var isFromCart: Boolean = false

    // Variables to store single item details for "Buy Now" to construct OrderItem
    private var buyNowProductName: String? = null
    private var buyNowProductPricePerUnit: Int = 0
    private var buyNowSelectedSize: String? = null
    private var buyNowQuantity: Int = 1 // Default quantity for buy now


    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("fil", "PH"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)
        supportActionBar?.hide()

        setupWindowInsets()
        initializeViews()
        setupRecyclerView()

        isFromCart = intent.getBooleanExtra("FROM_CART", false)
        Log.d("OrderActivity", "isFromCart: $isFromCart")

        if (isFromCart) {
            setupForCartCheckout()
        } else {
            setupForBuyNowCheckout()
        }

        setupDeliveryOptions()
        setupAddressEditing()
        setupBackButton()
        setupPlaceOrderButton()

        updateTotalsDisplay(isPickupSelected = pickupButton.isSelected)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.order_layout_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        Log.d("OrderActivity", "Initializing views...")
        toolbarTitleTextView = findViewById(R.id.text_details)
        buyNowItemsWrapper = findViewById(R.id.product_quantity_container_wrapper)
        productNameInContainerTextView = findViewById(R.id.detail_product_name)
        productImageView = findViewById(R.id.detail_product_image)
        productSizeView = findViewById(R.id.detail_product_size)
        plusButton = findViewById(R.id.imageButton3)
        minusButton = findViewById(R.id.imageButton5)
        quantityText = findViewById(R.id.quantity_text)
        orderSummaryRecyclerView = findViewById(R.id.recycler_view_order_summary_items)
        subtotalPriceTextView = findViewById(R.id.detail_product_price)
        deliveryFeeTextView = findViewById(R.id.delivery_fee_text)
        totalPriceTextView = findViewById(R.id.total_price_text)
        deliverButton = findViewById(R.id.deliver_button)
        pickupButton = findViewById(R.id.pickup_button)
        addressHeader = findViewById(R.id.delivery_address_header)
        editAddressButton = findViewById(R.id.edit_address_button)
        addressText = findViewById(R.id.address_text)
        placeOrderButton = findViewById(R.id.button_place_order)
        Log.d("OrderActivity", "Views initialized.")
    }

    private fun setupRecyclerView() {
        orderSummaryAdapter = OrderSummaryAdapter()
        orderSummaryRecyclerView.apply {
            adapter = orderSummaryAdapter
            layoutManager = LinearLayoutManager(this@OrderActivity)
        }
    }

    private fun setupForCartCheckout() {
        Log.d("OrderActivity", "Setting up for CART CHECKOUT.")
        toolbarTitleTextView.text = "Confirm Order"
        buyNowItemsWrapper.visibility = View.GONE
        orderSummaryRecyclerView.visibility = View.VISIBLE

        val cartItems = cartViewModel.cartItems.value ?: emptyList()
        orderSummaryAdapter.submitList(cartItems)
        Log.d("OrderActivity", "Cart items submitted to adapter: ${cartItems.size} items")

        currentSubtotal = cartViewModel.cartSubtotal.value ?: 0.0
        // subtotalPriceTextView.text = currencyFormatter.format(currentSubtotal) // updateTotalsDisplay handles this

        cartViewModel.cartItems.observe(this) { items ->
            if(isFromCart) {
                orderSummaryAdapter.submitList(items)
                Log.d("OrderActivity", "Live update: Cart items submitted to adapter: ${items.size} items")
            }
        }
        cartViewModel.cartSubtotal.observe(this) { subtotal ->
            if (isFromCart) {
                currentSubtotal = subtotal
                // subtotalPriceTextView.text = currencyFormatter.format(currentSubtotal) // updateTotalsDisplay handles this
                updateTotalsDisplay(isPickupSelected = pickupButton.isSelected)
            }
        }
    }

    private fun setupForBuyNowCheckout() {
        Log.d("OrderActivity", "Setting up for BUY NOW CHECKOUT.")
        toolbarTitleTextView.text = "Order Details"
        buyNowItemsWrapper.visibility = View.VISIBLE
        orderSummaryRecyclerView.visibility = View.GONE

        buyNowProductName = intent.getStringExtra("PRODUCT_NAME") ?: "Unknown Product"
        buyNowProductPricePerUnit = intent.getIntExtra("PRODUCT_PRICE", 0) // This is price per unit *after size adjustment* from ProductDetailActivity
        buyNowSelectedSize = intent.getStringExtra("PRODUCT_SIZE") ?: "N/A"
        val imageRes = intent.getIntExtra("PRODUCT_IMAGE", R.drawable.caffee_mocha)

        productImageView.setImageResource(imageRes)
        productSizeView.text = "Size: $buyNowSelectedSize"
        productNameInContainerTextView.text = buyNowProductName

        buyNowQuantity = 1 // Reset quantity for buy now
        quantityText.text = buyNowQuantity.toString()
        // For "Buy Now", currentSubtotal is pricePerUnit * quantity
        currentSubtotal = (buyNowQuantity * buyNowProductPricePerUnit).toDouble()


        plusButton.setOnClickListener {
            buyNowQuantity++
            quantityText.text = buyNowQuantity.toString()
            currentSubtotal = (buyNowQuantity * buyNowProductPricePerUnit).toDouble()
            updateTotalsDisplay(isPickupSelected = pickupButton.isSelected)
        }

        minusButton.setOnClickListener {
            if (buyNowQuantity > 1) {
                buyNowQuantity--
                quantityText.text = buyNowQuantity.toString()
                currentSubtotal = (buyNowQuantity * buyNowProductPricePerUnit).toDouble()
                updateTotalsDisplay(isPickupSelected = pickupButton.isSelected)
            }
        }
    }

    private fun updateTotalsDisplay(isPickupSelected: Boolean) {
        val feeForCalc = if (isPickupSelected) 0 else currentDeliveryFeeValue
        val grandTotal = currentSubtotal + feeForCalc

        subtotalPriceTextView.text = currencyFormatter.format(currentSubtotal)

        if (isPickupSelected) {
            val feeText = currencyFormatter.format(currentDeliveryFeeValue.toDouble())
            val spannable = SpannableString(feeText)
            spannable.setSpan(StrikethroughSpan(), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            deliveryFeeTextView.text = spannable
        } else {
            deliveryFeeTextView.text = currencyFormatter.format(feeForCalc.toDouble())
        }

        totalPriceTextView.text = currencyFormatter.format(grandTotal)
        val prefixTextView: TextView? = findViewById(R.id.total_price_text_currency_prefix)
//        prefixTextView?.text = currencyFormatter.currency?.symbol ?: "₱"

        Log.d("OrderActivity", "Totals updated: Subtotal=${currencyFormatter.format(currentSubtotal)}, DeliveryFeeRaw=$feeForCalc, GrandTotal=${currencyFormatter.format(grandTotal)}")
    }


    private fun setupPlaceOrderButton() {
        placeOrderButton.setOnClickListener {
            val currentUserId = Firebase.auth.currentUser?.uid
            if (currentUserId == null) {
                Toast.makeText(this, "Please login to place an order.", Toast.LENGTH_SHORT).show()
                // Optionally navigate to LoginActivity
                return@setOnClickListener
            }

            // 1. Gather Order Items (Your existing logic for this)
            val orderItemsList = mutableListOf<OrderItem>()
            if (isFromCart) {
                cartViewModel.cartItems.value?.forEach { cartItem ->
                    orderItemsList.add(
                        OrderItem(
                            productId = cartItem.productId,
                            productName = cartItem.productName,
                            selectedSize = cartItem.selectedSize,
                            quantity = cartItem.quantity,
                            pricePerUnit = cartItem.pricePerUnit
                        )
                    )
                }
            } else {
                if (buyNowProductName != null && buyNowSelectedSize != null) {
                    orderItemsList.add(
                        OrderItem(
                            productId = buyNowProductName!!,
                            productName = buyNowProductName!!,
                            selectedSize = buyNowSelectedSize!!,
                            quantity = buyNowQuantity,
                            pricePerUnit = buyNowProductPricePerUnit.toDouble()
                        )
                    )
                }
            }

            if (orderItemsList.isEmpty()) {
                Toast.makeText(this, "Your order is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Gather other details (Your existing logic)
            val deliveryMethod = if (pickupButton.isSelected) "Pickup" else "Delivery"
            var finalDeliveryAddress: String? = addressText.text.toString()
            if (deliveryMethod == "Pickup" || finalDeliveryAddress == "No Address Available" || finalDeliveryAddress.isNullOrBlank()) {
                finalDeliveryAddress = null
            }

            if (deliveryMethod == "Delivery" && finalDeliveryAddress == null) {
                Toast.makeText(this, "Please provide a delivery address.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val actualDeliveryFee = if (pickupButton.isSelected) 0.0 else currentDeliveryFeeValue.toDouble()
            val finalGrandTotal = currentSubtotal + actualDeliveryFee

            // 3. Create OrderDetails object (Your existing logic)
            val newOrder = OrderDetails(
                // orderId is auto-generated
                items = orderItemsList,
                subtotal = currentSubtotal,
                deliveryFee = actualDeliveryFee,
                grandTotal = finalGrandTotal,
                deliveryMethod = deliveryMethod,
                deliveryAddress = finalDeliveryAddress
                // orderTimestamp is omitted; Firestore will set it due to @ServerTimestamp
            )

            // --- 4. Save the order to Firestore ---
            placeOrderButton.isEnabled = false // Disable button to prevent multiple clicks
            val db = Firebase.firestore
            db.collection("users").document(currentUserId)
                .collection("orders").document(newOrder.orderId) // Use auto-generated orderId
                .set(newOrder) // Set the OrderDetails object
                .addOnSuccessListener {
                    Log.i("OrderPlacement", "Order successfully written to Firestore: ${newOrder.orderId}")

                    // 5. Show success dialog (Your existing logic)
                    AlertDialog.Builder(this)
                        .setTitle("Order Placed Successfully!")
                        .setMessage("Your Order ID: ${newOrder.orderId}\nTotal Amount: ${currencyFormatter.format(newOrder.grandTotal)}\nThank you for your order!")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            // 6. Post-order actions
                            if (isFromCart) {
                                cartViewModel.clearCart()
                            }
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
                .addOnFailureListener { e ->
                    Log.w("OrderPlacement", "Error writing order to Firestore", e)
                    Toast.makeText(baseContext, "Failed to place order: ${e.message}", Toast.LENGTH_LONG).show()
                    placeOrderButton.isEnabled = true // Re-enable button on failure
                }
        }
    }




    // --- Other methods (setupDeliveryOptions, setupAddressEditing, setupBackButton, showExitConfirmationDialog, idToString) ---
    // ... (These should be the same as the previous version you have) ...
    private fun setupDeliveryOptions() {
        val defaultDeliverAddress = "No Address Available"
        val pickupAddressText = "Kape PH - Marikina Branch"

        deliverButton.isSelected = true
        deliverButton.setTextColor(Color.WHITE)
        pickupButton.isSelected = false
        pickupButton.setTextColor(Color.BLACK)
        addressHeader.text = "Delivery Address"
        addressText.text = defaultDeliverAddress
        editAddressButton.isVisible = true

        deliverButton.setOnClickListener {
            if (!deliverButton.isSelected) {
                deliverButton.setTextColor(Color.WHITE)
                pickupButton.setTextColor(Color.BLACK)
                deliverButton.isSelected = true
                pickupButton.isSelected = false
                addressHeader.text = "Delivery Address"
                addressText.text = defaultDeliverAddress
                editAddressButton.isVisible = true
                updateTotalsDisplay(isPickupSelected = false)
            }
        }

        pickupButton.setOnClickListener {
            if (!pickupButton.isSelected) {
                pickupButton.setTextColor(Color.WHITE)
                deliverButton.setTextColor(Color.BLACK)
                pickupButton.isSelected = true
                deliverButton.isSelected = false
                addressHeader.text = "Pick Up At"
                addressText.text = pickupAddressText
                editAddressButton.isVisible = false
                updateTotalsDisplay(isPickupSelected = true)
            }
        }
    }

    private fun setupAddressEditing() {
        editAddressButton.setOnClickListener {
            showEditDialog(addressText)
        }
    }

    private fun showEditDialog(textView: TextView) {
        val currentText = textView.text.toString()
        val editText = EditText(this).apply {
            setText(if (currentText == "No Address Available" || currentText.isBlank()) "" else currentText)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            filters = arrayOf(InputFilter.LengthFilter(100))
            hint = "Enter delivery address"
            setTextColor(Color.BLACK)
            setPadding(70, 36, 16, 0)
            textSize = 16f
            background = null
            setHintTextColor(ContextCompat.getColor(this@OrderActivity, R.color.brown_01))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Delivery Address")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    textView.text = newText
                    Toast.makeText(this, "Address updated", Toast.LENGTH_SHORT).show()
                } else {
                    textView.text = "No Address Available"
                    Toast.makeText(this, "Address cannot be empty, reverted.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = editText.text.isNotEmpty()
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = s?.isNotBlank() == true
            }
        })
    }

    private fun setupBackButton() {
        val backButtonImpl: ImageButton = findViewById(R.id.back_Button)
        backButtonImpl.setOnClickListener {
            showExitConfirmationDialog()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this@OrderActivity)
            .setTitle("Confirm Exit")
            .setMessage("Return to previous page? Order details won't be saved.")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .setCancelable(true)
            .show()
    }

    fun View.idToString(): String {
        return try {
            resources.getResourceEntryName(id)
        } catch (e: Exception) {
            "ID_NOT_FOUND ($id)"
        }
    }
}