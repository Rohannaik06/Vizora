// ================= INITIALIZATION (सर्व्हिस सुरू करताना) =================
document.addEventListener("DOMContentLoaded", function() {
    checkLoginState();
    fetchProducts();
    setInterval(changeSlide, 3000);
    loadCartCount();
});

// ================= LOGIN / LOGOUT =================
function checkLoginState() {
    const login = localStorage.getItem("isLoggedIn");
    const beforeLogin = document.getElementById("beforeLogin");
    const afterLogin = document.getElementById("afterLogin");

    if (beforeLogin && afterLogin) {
        beforeLogin.style.display = (login === "true") ? "none" : "flex";
        afterLogin.style.display = (login === "true") ? "flex" : "none";
    }

    const username = localStorage.getItem("userName");
    const usernameElem = document.getElementById("username");
    if (username && usernameElem) {
        usernameElem.innerHTML = username;
    }
}

function logout() {
    localStorage.clear(); // सर्व जुना डेटा (isLoggedIn, userId) डिलीट करेल
    window.location.href = "index.html";
}

// ================= SLIDER =================
let currentSlide = 0;
function changeSlide() {
    const slides = document.querySelectorAll(".slide");
    if (slides.length === 0) return;
    
    slides[currentSlide].classList.remove("active");
    currentSlide = (currentSlide + 1) % slides.length;
    slides[currentSlide].classList.add("active");
}

// ================= FETCH PRODUCTS =================
function fetchProducts() {
    fetch("ProductServlet")
    .then(response => response.json())
    .then(products => {
        const grid = document.getElementById("product-grid");
        if (!grid) return;
        
        grid.innerHTML = "";

        products.forEach(product => {
            // Stock Logic
            let stockHTML = "";
            if (product.stock === 0) {
                stockHTML = `<span class="stock out">Out of Stock</span>`;
            } else if (product.stock <= 10) {
                stockHTML = `<span class="stock low">Only ${product.stock} left</span>`;
            } else {
                stockHTML = `<span class="stock in">In Stock</span>`;
            }

            // Badges
            let featured = product.featured ? `<span class="badge featured">Featured</span>` : "";
            let polarized = product.polarized ? `<span class="badge polarized">Polarized</span>` : "";
            let discount = product.discount > 0 ? `<div class="discount">${product.discount}% OFF</div>` : "";

            // Safely handling null values
            let cat = product.category ? product.category.toLowerCase() : "";
            let gen = product.gender ? product.gender.toLowerCase() : "";
            let pol = product.polarized ? "polarized" : "";

           grid.innerHTML += `
                <div class="product-card" 
                    data-id="${product.id}" 
                    data-category="${product.category ? product.category.toLowerCase() : ''}"
                    data-name="${product.name.toLowerCase()}" 
                    data-brand="${product.brand ? product.brand.toLowerCase() : ''}">
                
                    <div class="img-container">
                        <div class="wishlist" onclick="toggleWishlist(${product.id}, this)">
                            <i class="fa-regular fa-heart"></i>
                        </div>
                        
                        <img src="images/${product.thumbnail}" alt="${product.name}">
                    </div>
                    
                    <div class="card-details">
                        <h3 class="product-title">${product.name}</h3>
                        <div class="price-row">
                            <span class="price">₹${product.sellingPrice}</span>
                            ${product.originalPrice > product.sellingPrice ? `<span class="original">₹${product.originalPrice}</span>` : ''}
                        </div>
                    </div>
                    
                    <div class="button-group">
                        <button class="btn-add" onclick="addToCart(${product.id})">Add to Cart</button>
                        <button class="btn-buy" onclick="SeeDetails(${product.id})">See Details</button>
                    </div>
                </div>
            `;
        });
    })
    .catch(error => console.error("Error fetching products:", error));
}

// ================= SEE DETAILS =================
function SeeDetails(productId) {
    if (productId) {
        // Redirects the user to the product details page with the specific ID
        window.location.href = "product.html?id=" + productId;
    } else {
        console.error("Product ID is missing.");
    }
}

// ================= SEARCH PRODUCTS =================
function searchProducts() {
    const input = document.getElementById("searchInput").value.toLowerCase();
    const cards = document.querySelectorAll(".product-card");

    cards.forEach(card => {
        // तुमच्या HTML मध्ये data attributes बरोबर आहेत याची खात्री करा
        const name = card.dataset.name || "";
        const brand = card.dataset.brand || "";
        const category = card.dataset.category || "";

        if (name.includes(input) || brand.includes(input) || category.includes(input)) {
            card.style.display = ""; // हे सर्वात सेफ आहे, कारण ते CSS मधील मूळ प्रॉपर्टी वापरते
        } else {
            card.style.display = "none";
        }
    });
}

// ================= FILTER =================
function filterProducts(category, btnElement) {
    // जर btnElement पास केला नसेल, तर एरर येऊ नये म्हणून event.target वापरणे
    let targetBtn = btnElement || event.target;
    
    document.querySelectorAll(".filter-bar button").forEach(btn => btn.classList.remove("active"));
    if (targetBtn && targetBtn.classList) {
        targetBtn.classList.add("active");
    }

    const cards = document.querySelectorAll(".product-card");
    category = category.toLowerCase();

    cards.forEach(card => {
        if (category === "all" || card.dataset.category.includes(category)) {
            card.style.display = "flex"; // CSS नुसार flex/block ठेवा
        } else {
            card.style.display = "none";
        }
    });
}

// ================= WISHLIST & PRODUCT CLICK =================
document.addEventListener("click", function(e) {
    // Wishlist Toggle
    if (e.target.classList.contains("fa-heart")) {
        e.target.classList.toggle("fa-regular");
        e.target.classList.toggle("fa-solid");
        e.target.parentElement.classList.toggle("active");
        return; 
    }

    // Product Card Click (Redirect to details)
    const card = e.target.closest(".product-card");
    if (!card) return;
    
    // जर युजरने Add to Cart किंवा Buy Now वर क्लिक केले असेल तर पेज बदलू नका
    if (e.target.closest(".btn-add") || e.target.closest(".btn-buy") || e.target.closest(".wishlist")) {
        return;
    }

    // Future: window.location = "product.html?id=" + card.dataset.id;
    console.log("Card Clicked:", card.dataset.id);
});

// ================= ADD TO CART (सुधारित) =================
function addToCart(productId) {
    const userId = localStorage.getItem("userId");

    if (!userId) {
        alert("Please login first.");
        window.location.href = "login.html";
        return;
    }

    fetch("CartServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "userId=" + userId + "&productId=" + productId
    })
    .then(response => response.text())
    .then(data => {
        if (data.trim() === "success") {
            alert("Product added to cart!");
            // इथे फंक्शन कॉल करा
            updateCartCount(); 
        } else {
            alert("Unable to add product.");
        }
    });
}

// ================= BUY NOW =================
function buyNow(id) {
    localStorage.setItem("buyNowProduct", id);
    alert("Proceeding to Checkout...");
    // Future: window.location = "checkout.html";
}

// ================= UPDATE CART COUNT (डायनॅमिक) =================
function updateCartCount() {
    const userId = localStorage.getItem("userId");
    if (!userId) return;

    // ViewCartServlet वरून डेटा फेच करा
    fetch(`ViewCartServlet?userId=${userId}`)
    .then(response => response.json())
    .then(cartItems => {
        const cartBtn = document.querySelector(".cart-btn");
        
        // कार्ट मधील सर्व आयटम्सच्या quantity ची बेरीज करा
        let totalCount = cartItems.reduce((sum, item) => sum + item.quantity, 0);
        
        // बटन टेक्स्ट अपडेट करा
        cartBtn.innerHTML = `<i class="fa-solid fa-cart-shopping"></i> Cart (${totalCount})`;
    });
}

// पेज लोड होताना सुद्धा एकदा अपडेट करा
document.addEventListener("DOMContentLoaded", updateCartCount);

// ================= NOTIFICATION =================
const notifyBtn = document.querySelector(".notify-btn");
if (notifyBtn) {
    notifyBtn.onclick = function() {
        alert("No new notifications.");
    };
}


// ================= TOGGLE WISHLIST =================
function toggleWishlist(productId, element) {
    const userId = localStorage.getItem("userId");

    // युजर लॉगिन नसेल तर आधी लॉगिन करायला सांगा
    if (!userId) {
        alert("Please login to add items to your wishlist.");
        window.location.href = "login.html";
        return;
    }

    // इव्हेंट बबलिंग थांबवण्यासाठी (जेणेकरून कार्डवर क्लिक केल्यासारखे वाटणार नाही)
    event.stopPropagation();

    fetch("WishlistServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `userId=${userId}&productId=${productId}`
    })
    .then(response => response.text())
    .then(data => {
        const icon = element.querySelector("i");
        
        if (data.trim() === "added") {
            // विशलिस्ट मध्ये ॲड झाले (लाल रंग आणि सॉलिड हार्ट)
            element.classList.add("active");
            icon.classList.remove("fa-regular");
            icon.classList.add("fa-solid");
            icon.style.color = "#e53935"; // Red color
            
        } else if (data.trim() === "removed") {
            // विशलिस्ट मधून काढले (नॉर्मल रंग आणि आउटलाईन हार्ट)
            element.classList.remove("active");
            icon.classList.remove("fa-solid");
            icon.classList.add("fa-regular");
            icon.style.color = "#777"; // Default color
            
        } else {
            alert("Something went wrong!");
        }
    })
    .catch(error => console.error("Error updating wishlist:", error));
}


// ================= WISHLIST & PRODUCT CARD CLICK =================
document.addEventListener("click", function(e) {
    
    // 1. If the user clicked outside a product card, do nothing
    const card = e.target.closest(".product-card");
    if (!card) return;
    
    // 2. If the user clicked Add to Cart, Buy Now, or the Wishlist Heart, DO NOT redirect.
    // Let their respective onclick functions do the work.
    if (e.target.closest(".btn-add") || e.target.closest(".btn-buy") || e.target.closest(".wishlist")) {
        return;
    }

    // 3. Otherwise, if they clicked anywhere else on the image or text, Redirect!
    const productId = card.dataset.id;
    if (productId) {
        window.location.href = "product.html?id=" + productId;
    } else {
        console.error("Error: Product ID is missing from the card's dataset.");
    }
});