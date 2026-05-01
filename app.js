document.addEventListener('DOMContentLoaded', () => {
    // UI Elements
    const scannerSection = document.getElementById('scanner-section');
    const resultsSection = document.getElementById('results-section');
    const loadingEl = document.getElementById('loading');
    
    // Buttons
    const searchBtn = document.getElementById('search-btn');
    const backBtn = document.getElementById('back-btn');
    const manualBarcodeInput = document.getElementById('manual-barcode');

    // Initialize Vanta 3D Background
    if (window.VANTA) {
        window.vantaEffect = VANTA.NET({
            el: "#vanta-bg",
            mouseControls: true,
            touchControls: true,
            gyroControls: false,
            minHeight: 200.00,
            minWidth: 200.00,
            scale: 1.00,
            scaleMobile: 1.00,
            color: 0x10b981,
            backgroundColor: 0x0f172a,
            points: 12.00,
            maxDistance: 20.00,
            spacing: 20.00
        });
    }

    // Html5Qrcode Scanner instance
    let html5QrcodeScanner;

    function initScanner() {
        html5QrcodeScanner = new Html5QrcodeScanner(
            "reader",
            { fps: 10, qrbox: {width: 250, height: 250} },
            /* verbose= */ false
        );
        html5QrcodeScanner.render(onScanSuccess, onScanFailure);
    }

    function onScanSuccess(decodedText, decodedResult) {
        console.log(`Scan result: ${decodedText}`);
        // Stop scanner immediately after finding a barcode
        if(html5QrcodeScanner) {
            html5QrcodeScanner.clear().catch(error => {
                console.error("Failed to clear html5QrcodeScanner. ", error);
            });
        }
        fetchDataAndShowResults(decodedText);
    }

    function onScanFailure(error) {
        // handle scan failure, usually better to ignore and keep scanning
        // console.warn(`Code scan error = ${error}`);
    }

    // Manual Entry
    searchBtn.addEventListener('click', () => {
        const barcode = manualBarcodeInput.value.trim();
        if (barcode) {
            if(html5QrcodeScanner) {
                html5QrcodeScanner.clear();
            }
            fetchDataAndShowResults(barcode);
        } else {
            alert('Please enter a valid barcode.');
        }
    });

    // Back button
    backBtn.addEventListener('click', () => {
        resultsSection.classList.add('hidden');
        scannerSection.classList.remove('hidden');
        manualBarcodeInput.value = '';
        initScanner();
    });

    // Fetch API Data
    async function fetchDataAndShowResults(barcode) {
        scannerSection.classList.add('hidden');
        loadingEl.classList.remove('hidden');
        
        // Clear UI first
        document.getElementById('product-name').textContent = 'Loading...';
        document.getElementById('product-brand').textContent = '-';
        document.getElementById('product-origin').textContent = '-';
        document.getElementById('product-weight').textContent = '-';
        document.getElementById('product-img').style.display = 'none';
        document.getElementById('total-co2').textContent = '0';
        document.getElementById('eco-rating').textContent = '?';
        document.getElementById('timeline-container').innerHTML = '';
        
        scannerSection.classList.remove('hidden'); // Keep scanner container visible but show loading

        try {
            // Use absolute URL pointing to our Express backend
            const backendUrl = window.location.origin.includes('localhost') || window.location.origin.includes('127.0.0.1')
                ? 'http://localhost:3000'
                : ''; // fallback relative if hosted together

            const [productRes, supplyChainRes] = await Promise.all([
                fetch(`${backendUrl}/api/product/${barcode}`).then(r => r.json()),
                fetch(`${backendUrl}/api/supply-chain/${barcode}`).then(r => r.json())
            ]);

            loadingEl.classList.add('hidden');
            scannerSection.classList.add('hidden');
            resultsSection.classList.remove('hidden');

            populateResults(productRes, supplyChainRes, barcode);

        } catch (error) {
            console.error("Error fetching data:", error);
            alert("Failed to connect to the backend server. Make sure it's running on port 3000.");
            loadingEl.classList.add('hidden');
            scannerSection.classList.remove('hidden');
            initScanner();
        }
    }

    function populateResults(productData, scData, barcode) {
        // 1. Populate Product Details
        if (productData.success && productData.product) {
            const p = productData.product;
            
            const badgeHtml = productData.mock 
                ? '<span style="font-size:0.6em; background:var(--warning); color:#fff; padding:3px 8px; border-radius:12px; margin-left:10px; vertical-align:middle;">Simulated Data</span>' 
                : `<a href="https://world.openfoodfacts.org/product/${barcode}" target="_blank" style="text-decoration:none; font-size:0.6em; background:var(--success); color:#fff; padding:3px 8px; border-radius:12px; margin-left:10px; vertical-align:middle; transition: opacity 0.2s; display: inline-flex; align-items: center; gap: 4px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">Verified <i class="fa-solid fa-arrow-up-right-from-square" style="font-size:0.8em"></i></a>`;
                
            document.getElementById('product-name').innerHTML = p.name + badgeHtml;
            document.getElementById('product-brand').textContent = p.brand;
            document.getElementById('product-origin').textContent = p.origin;
            document.getElementById('product-weight').textContent = p.weight;
            
            const img = document.getElementById('product-img');
            if (p.image) {
                img.src = p.image;
                img.style.display = 'block';
            } else {
                img.style.display = 'none';
            }
        }

        // 2. Populate Eco Metrics
        if (scData.success) {
            document.getElementById('total-co2').textContent = scData.totalCo2EmissionsKg;
            
            const ratingSpan = document.getElementById('eco-rating');
            ratingSpan.textContent = scData.ecoRating;
            
            // Update color based on rating
            const circle = document.getElementById('eco-rating-circle');
            let color = 'var(--success)';
            let hexColor = 0x10b981; // Green
            
            if (scData.ecoRating === 'B') {
                color = 'var(--warning)';
                hexColor = 0xf59e0b; // Orange
            }
            if (scData.ecoRating === 'C') {
                color = 'var(--danger)';
                hexColor = 0xef4444; // Red
            }
            
            circle.style.background = `conic-gradient(${color} 100%, transparent 0)`;
            ratingSpan.style.color = color;

            // Dynamically change the 3D Vanta background color!
            if (window.vantaEffect) {
                window.vantaEffect.setOptions({
                    color: hexColor
                });
            }

            document.getElementById('network-name').textContent = scData.blockchainNetwork;

            // 3. Populate Timeline
            const timelineContainer = document.getElementById('timeline-container');
            timelineContainer.innerHTML = ''; // clear existing

            scData.stages.forEach((stage, index) => {
                const date = new Date(stage.date).toLocaleDateString();
                
                // Assign a unique ID so we can apply the scramble effect later
                const hashId = 'hash-' + index;
                const detailsId = 'details-' + index;

                let detailsHtml = '<ul class="details-list">';
                if (stage.details) {
                    for (const [key, value] of Object.entries(stage.details)) {
                        detailsHtml += `<li><strong>${key}:</strong> ${value}</li>`;
                    }
                }
                detailsHtml += '</ul>';
                
                const html = `
                    <div class="timeline-item" data-tilt data-tilt-max="5" data-tilt-speed="400" style="animation-delay: ${index * 0.4}s">
                        <div class="timeline-dot"></div>
                        <div class="timeline-content">
                            <div class="timeline-header">
                                <h5>${stage.stage}</h5>
                                <span class="timeline-date">${date}</span>
                            </div>
                            <div class="timeline-body">
                                <div><i class="fa-solid fa-location-dot"></i> ${stage.location}</div>
                                <div><i class="fa-solid fa-cloud"></i> ${stage.co2EmissionsKg.toFixed(2)} kg CO2</div>
                            </div>
                            <div class="tx-hash">
                                <i class="fa-solid fa-check-circle"></i> <span id="${hashId}" data-hash="${stage.blockchainTxHash}"></span>
                            </div>
                            <button class="btn-details" data-target="${detailsId}">View Details <i class="fa-solid fa-chevron-down"></i></button>
                            <div id="${detailsId}" class="stage-details">
                                ${detailsHtml}
                            </div>
                        </div>
                    </div>
                `;
                timelineContainer.insertAdjacentHTML('beforeend', html);
                
                // Trigger Cyberpunk Scramble Effect with a delay based on the timeline animation
                setTimeout(() => {
                    scrambleText(document.getElementById(hashId), stage.blockchainTxHash);
                }, index * 400 + 400);
            });
            
            // Re-init vanilla tilt on new timeline items
            if (window.VanillaTilt) {
                VanillaTilt.init(document.querySelectorAll(".timeline-item"));
            }

            // Add toggle event listeners to details buttons
            document.querySelectorAll('.btn-details').forEach(btn => {
                btn.addEventListener('click', function() {
                    const detailsDiv = document.getElementById(this.dataset.target);
                    if (detailsDiv.classList.contains('open')) {
                        detailsDiv.classList.remove('open');
                        this.innerHTML = 'View Details <i class="fa-solid fa-chevron-down"></i>';
                    } else {
                        detailsDiv.classList.add('open');
                        this.innerHTML = 'Hide Details <i class="fa-solid fa-chevron-up"></i>';
                    }
                });
            });
        }
    }

    // Cyberpunk Decrypt Text Effect
    function scrambleText(element, finalString) {
        const chars = '0123456789ABCDEF!@#$%^&*()';
        let iterations = 0;
        const maxIterations = 20;
        
        const interval = setInterval(() => {
            element.innerText = finalString.split('').map((letter, index) => {
                if(index < iterations) {
                    return finalString[index];
                }
                return chars[Math.floor(Math.random() * chars.length)];
            }).join('');
            
            if(iterations >= finalString.length) {
                clearInterval(interval);
            }
            iterations += 1/2; // Adjust speed of decryption
        }, 30);
    }

    // Start scanner on load
    initScanner();
});
