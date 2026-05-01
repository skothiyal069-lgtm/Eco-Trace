const express = require('express');
const cors = require('cors');
const axios = require('axios');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use(express.static('.')); // Serve static files from current directory

// Helper function to create mock blockchain hashes
function generateMockHash(data) {
    return crypto.createHash('sha256').update(JSON.stringify(data)).digest('hex');
}

// 1. Endpoint to fetch product details using Open Food Facts API
app.get('/api/product/:barcode', async (req, res) => {
    try {
        const { barcode } = req.params;
        // https://world.openfoodfacts.org/api/v2/product/[barcode].json
        const response = await axios.get(`https://world.openfoodfacts.org/api/v2/product/${barcode}.json`, {
            validateStatus: function (status) {
                return status < 500; // resolve for 404 as well
            }
        });
        
        if (response.status === 200 && response.data && response.data.status === 1) {
            const product = response.data.product;
            res.json({
                success: true,
                mock: false,
                product: {
                    name: product.product_name || 'Unknown Product',
                    brand: product.brands || 'Unknown Brand',
                    image: product.image_url,
                    ingredients: product.ingredients_text || 'No ingredients listed',
                    origin: product.origins || 'Unknown origin',
                    weight: product.quantity || 'Unknown weight'
                }
            });
        } else {
            // Fallback for demonstration if barcode not found
            const baseNum = parseInt(barcode.replace(/\\D/g, '').substring(0, 6)) || 123456;
            res.json({
                success: true,
                mock: true,
                product: {
                    name: `Eco Item ${baseNum}`,
                    brand: `Global Corp ${baseNum % 100}`,
                    image: `https://via.placeholder.com/150/${(baseNum*123).toString(16).substring(0,6).padStart(6, '0')}/FFFFFF?text=Product`,
                    ingredients: 'Recycled Materials, Natural Fibers, Polymers',
                    origin: ['USA', 'China', 'Germany', 'Mexico', 'India', 'Brazil'][baseNum % 6],
                    weight: `${(baseNum % 1900) + 100}g` // Dynamic weight between 100g and 2000g
                }
            });
        }
    } catch (error) {
        console.error("Error fetching product data:", error.message);
        const baseNum = parseInt(req.params.barcode.replace(/\\D/g, '').substring(0, 6)) || 123456;
        res.json({
            success: true,
            mock: true,
            product: {
                name: `Eco Item ${baseNum}`,
                brand: `Global Corp ${baseNum % 100}`,
                image: `https://via.placeholder.com/150/${(baseNum*123).toString(16).substring(0,6).padStart(6, '0')}/FFFFFF?text=Product`,
                ingredients: 'Recycled Materials, Natural Fibers, Polymers',
                origin: ['USA', 'China', 'Germany', 'Mexico', 'India', 'Brazil'][baseNum % 6],
                weight: `${(baseNum % 1900) + 100}g`
            }
        });
    }
});

// 2. Endpoint to fetch simulated supply chain history and CO2 footprint
app.get('/api/supply-chain/:barcode', (req, res) => {
    const { barcode } = req.params;
    
    // Simulate a supply chain footprint based on the barcode string to have deterministic results
    const baseNum = parseInt(barcode.replace(/\D/g, '').substring(0, 5)) || 12345;
    
    const stages = [
        {
            stage: "Raw Material Extraction",
            location: "Farm A, Country X",
            date: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
            co2EmissionsKg: (baseNum % 5) + 1.2,
            pollutionIndex: "Low",
            details: {
                "Method": "Sustainable Farming",
                "Water Usage": `${(baseNum % 100) + 50} Liters`,
                "Soil Quality": "Grade A"
            }
        },
        {
            stage: "Manufacturing",
            location: "Factory B, Country Y",
            date: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000).toISOString(),
            co2EmissionsKg: (baseNum % 10) + 5.5,
            pollutionIndex: "Medium",
            details: {
                "Energy Source": "70% Solar, 30% Grid",
                "Waste Recycled": `${80 + (baseNum % 15)}%`,
                "Certifications": "ISO 14001, FairTrade"
            }
        },
        {
            stage: "Transportation",
            location: "Logistics Hub C",
            date: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
            co2EmissionsKg: (baseNum % 8) + 3.0,
            pollutionIndex: "High",
            details: {
                "Mode": baseNum % 2 === 0 ? "Ocean Freight & Rail" : "Air & Truck",
                "Distance": `${(baseNum % 5000) + 1000} km`,
                "Fuel Type": "Bio-Diesel Blend",
                "Carrier": "EcoTrans Logistics"
            }
        },
        {
            stage: "Retail Checkout",
            location: "Local Store",
            date: new Date().toISOString(),
            co2EmissionsKg: 0.5,
            pollutionIndex: "Low",
            details: {
                "Packaging": "100% Recyclable Cardboard",
                "Storage": "Ambient Temperature",
                "Local Transport": `${(baseNum % 50) + 5} km via EV Van`
            }
        }
    ];

    // Add mock blockchain transaction hashes for verification
    const verifiedStages = stages.map(stage => {
        return {
            ...stage,
            blockchainTxHash: '0x' + generateMockHash(stage).substring(0, 40)
        };
    });

    const totalCo2 = verifiedStages.reduce((sum, stage) => sum + stage.co2EmissionsKg, 0);

    res.json({
        success: true,
        blockchainNetwork: "Ethereum (Simulated)",
        contractAddress: "0xMockContract1234567890abcdef1234567890",
        totalCo2EmissionsKg: totalCo2.toFixed(2),
        ecoRating: totalCo2 < 15 ? 'A' : (totalCo2 < 25 ? 'B' : 'C'),
        stages: verifiedStages
    });
});

app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});
