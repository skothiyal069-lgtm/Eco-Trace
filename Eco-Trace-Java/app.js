document.addEventListener('DOMContentLoaded', () => {
    // UI Elements
    const scannerSection = document.getElementById('scanner-section');
    const resultsSection = document.getElementById('results-section');
    const crowdsourceSection = document.getElementById('crowdsource-section');
    const loadingEl = document.getElementById('loading');
    
    // Buttons
    const searchBtn = document.getElementById('search-btn');
    const backBtn = document.getElementById('back-btn');
    const csBackBtn = document.getElementById('cs-back-btn');
    const submitCsBtn = document.getElementById('submit-cs-btn');
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

    function isJson(str) {
        try {
            JSON.parse(str);
            return true;
        } catch (e) {
            return false;
        }
    }

    function onScanSuccess(decodedText, decodedResult) {
        console.log(`Scan result: ${decodedText}`);
        // Stop scanner immediately after finding a barcode
        if(html5QrcodeScanner) {
            html5QrcodeScanner.clear().catch(error => {
                console.error("Failed to clear html5QrcodeScanner. ", error);
            });
        }
        
        if (isJson(decodedText)) {
            handleQrJsonData(JSON.parse(decodedText));
        } else {
            fetchDataAndShowResults(decodedText);
        }
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
            if (isJson(barcode)) {
                handleQrJsonData(JSON.parse(barcode));
            } else {
                fetchDataAndShowResults(barcode);
            }
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

    csBackBtn.addEventListener('click', () => {
        crowdsourceSection.classList.add('hidden');
        scannerSection.classList.remove('hidden');
        manualBarcodeInput.value = '';
        initScanner();
    });

    // Submit Crowdsource
    submitCsBtn.addEventListener('click', async () => {
        const barcode = document.getElementById('cs-barcode').value;
        const name = document.getElementById('cs-name').value.trim();
        const brand = document.getElementById('cs-brand').value.trim();

        if (!name) {
            alert('Please enter a product name');
            return;
        }

        const btnOriginalText = submitCsBtn.innerHTML;
        submitCsBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Submitting...';
        
        try {
            const backendUrl = window.location.origin.includes('localhost') || window.location.origin.includes('127.0.0.1')
                ? 'http://localhost:8080'
                : '';
                
            await fetch(`${backendUrl}/api/product/`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ barcode, name, brand })
            });

            submitCsBtn.innerHTML = '<i class="fa-solid fa-check"></i> Thanks!';
            setTimeout(() => {
                submitCsBtn.innerHTML = btnOriginalText;
                crowdsourceSection.classList.add('hidden');
                // Automatically fetch the newly added data
                fetchDataAndShowResults(barcode);
            }, 1000);
        } catch(e) {
            alert('Failed to submit product data.');
            submitCsBtn.innerHTML = btnOriginalText;
        }
    });

    function handleQrJsonData(data) {
        scannerSection.classList.add('hidden');
        loadingEl.classList.remove('hidden');
        
        // Product format expected: { name, brand, origin, ..., stages: [...] }
        const productRes = {
            success: true,
            dataQuality: 'verified',
            product: data
        };
        
        const supplyChainRes = {
            success: !!(data.stages && data.stages.length > 0),
            blockchainNetwork: 'Digital Product Passport (Direct)',
            totalCo2EmissionsKg: data.stages ? data.stages.reduce((sum, s) => sum + (s.co2EmissionsKg || 0), 0).toFixed(2) : '0',
            ecoRating: 'A',
            stages: data.stages || []
        };
        
        setTimeout(() => {
            loadingEl.classList.add('hidden');
            scannerSection.classList.add('hidden');
            resultsSection.classList.remove('hidden');
            populateResults(productRes, supplyChainRes, 'QR_CODE');
        }, 800);
    }

    // Fetch API Data
    async function fetchDataAndShowResults(barcode) {
        scannerSection.classList.add('hidden');
        loadingEl.classList.remove('hidden');
        
        // Clear UI first
        document.getElementById('product-name').textContent = 'Loading...';
        document.getElementById('product-brand').textContent = '-';
        document.getElementById('product-manufacturer').textContent = '-';
        document.getElementById('product-category').textContent = '-';
        document.getElementById('product-origin').textContent = '-';
        document.getElementById('product-weight').textContent = '-';
        document.getElementById('product-packaging').textContent = '-';
        document.getElementById('product-certifications').textContent = '-';
        document.getElementById('product-allergens').textContent = '-';
        document.getElementById('product-sources').textContent = '-';
        document.getElementById('product-description').textContent = 'Fetching the most accurate product profile available.';
        document.getElementById('data-quality-badge').textContent = 'Loading...';
        document.getElementById('data-quality-badge').className = 'badge';
        document.getElementById('product-img').style.display = 'none';
        document.getElementById('total-co2').textContent = '0';
        document.getElementById('eco-rating').textContent = '?';
        document.getElementById('timeline-container').innerHTML = '';
        
        scannerSection.classList.remove('hidden'); // Keep scanner container visible but show loading

        try {
            // Use absolute URL pointing to our Java backend
            const backendUrl = window.location.origin.includes('localhost') || window.location.origin.includes('127.0.0.1')
                ? 'http://localhost:8080'
                : ''; // fallback relative if hosted together

            const productResResponse = await fetch(`${backendUrl}/api/product/${barcode}`);
            const productRes = await productResResponse.json();

            if (!productRes.success) {
                // Show Crowdsource Section
                loadingEl.classList.add('hidden');
                scannerSection.classList.add('hidden');
                crowdsourceSection.classList.remove('hidden');
                document.getElementById('cs-barcode').value = barcode;
                return; // Stop here
            }

            const supplyChainRes = await fetch(`${backendUrl}/api/supply-chain/${barcode}`).then(r => r.json());

            loadingEl.classList.add('hidden');
            scannerSection.classList.add('hidden');
            resultsSection.classList.remove('hidden');

            populateResults(productRes, supplyChainRes, barcode);

        } catch (error) {
            console.error("Error fetching data:", error);
            alert("Failed to connect to the backend server. Make sure Java Server is running on port 8080.");
            loadingEl.classList.add('hidden');
            scannerSection.classList.remove('hidden');
            initScanner();
        }
    }

    function populateResults(productData, scData, barcode) {
        const p = productData.success && productData.product ? productData.product : {};
        const quality = productData.dataQuality || (productData.mock ? 'estimated' : 'partial');

        const qualityBadge = document.getElementById('data-quality-badge');
        if (quality === 'verified') {
            qualityBadge.textContent = 'Verified data';
            qualityBadge.className = 'badge';
        } else if (quality === 'partial') {
            qualityBadge.textContent = 'Partial data';
            qualityBadge.className = 'badge warning';
        } else {
            qualityBadge.textContent = 'Estimated data';
            qualityBadge.className = 'badge danger';
        }

        document.getElementById('product-name').textContent = p.name || 'Unknown Product';
        document.getElementById('product-brand').textContent = p.brand || '-';
        document.getElementById('product-manufacturer').textContent = p.manufacturer || '-';
        document.getElementById('product-category').textContent = p.category || '-';
        document.getElementById('product-origin').textContent = p.origin || '-';
        document.getElementById('product-weight').textContent = p.weight || '-';
        document.getElementById('product-packaging').textContent = p.packaging || '-';
        document.getElementById('product-certifications').textContent = p.certifications || '-';
        document.getElementById('product-allergens').textContent = p.allergens || '-';
        document.getElementById('product-sources').textContent = Array.isArray(p.sources) ? p.sources.join(' · ') : (p.sources || '-');
        document.getElementById('product-description').textContent = p.description || 'No product description available.';

        const img = document.getElementById('product-img');
        if (p.image) {
            img.src = p.image;
            img.style.display = 'block';
        } else {
            img.style.display = 'none';
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

            // Render 3D Globe
            renderGlobe(scData.stages);

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
        } else {
            document.getElementById('total-co2').textContent = '0';
            document.getElementById('eco-rating').textContent = '?';
            document.getElementById('network-name').textContent = 'Unavailable';
            document.getElementById('timeline-container').innerHTML = '';
            renderGlobe(null);
        }
    }

    // Cyberpunk Decrypt Text Effect
    function scrambleText(element, finalString) {
        if (!element || !finalString) return;
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

    let myGlobe = null;

    function renderGlobe(stages) {
        const globeContainer = document.getElementById('globeViz');
        const fallback = document.getElementById('globe-fallback');
        const fallbackText = document.getElementById('globe-fallback-text');
        
        if (!stages || stages.length === 0) {
            fallback.classList.remove('hidden');
            fallbackText.textContent = "Detailed supply chain metrics not available for standard barcodes. Please scan a Digital Product Passport QR.";
            if (myGlobe) {
                // Remove existing globe canvas
                const canvas = globeContainer.querySelector('canvas');
                if (canvas) canvas.remove();
                myGlobe = null;
            }
            return;
        }
        
        fallback.classList.add('hidden');
        
        const gData = stages.map((s, i) => {
            const nextStage = stages[i+1];
            if (!nextStage) return null;
            return {
                startLat: s.lat || (Math.random() - 0.5) * 160,
                startLng: s.lng || (Math.random() - 0.5) * 360,
                endLat: nextStage.lat || (Math.random() - 0.5) * 160,
                endLng: nextStage.lng || (Math.random() - 0.5) * 360,
                color: ['#10b981', '#0ea5e9', '#8b5cf6'][Math.floor(Math.random() * 3)]
            };
        }).filter(d => d !== null);

        if (myGlobe) {
            myGlobe.arcsData(gData);
        } else {
            // Remove fallback elements completely from globe area for rendering
            const fallbackClone = fallback.cloneNode(true);
            globeContainer.innerHTML = '';
            globeContainer.appendChild(fallbackClone);

            myGlobe = Globe()
                (globeContainer)
                .globeImageUrl('https://unpkg.com/three-globe/example/img/earth-night.jpg')
                .arcsData(gData)
                .arcColor('color')
                .arcDashLength(0.4)
                .arcDashGap(0.2)
                .arcDashAnimateTime(1500)
                .backgroundColor('rgba(0,0,0,0)');
                
            // Resize observer
            const ro = new ResizeObserver(() => {
                myGlobe.width(globeContainer.clientWidth);
                myGlobe.height(globeContainer.clientHeight);
            });
            ro.observe(globeContainer);
            
            // Auto-rotate
            myGlobe.controls().autoRotate = true;
            myGlobe.controls().autoRotateSpeed = 1.5;
        }
    }

    // Start scanner on load
    initScanner();
});
