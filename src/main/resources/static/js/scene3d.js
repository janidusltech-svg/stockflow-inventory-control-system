(function () {
    "use strict";

    var canvas = document.getElementById("scene3d-canvas");
    var stage = canvas ? canvas.closest(".scene3d-stage") : null;
    if (!stage || !canvas) return;

    var reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    var clamp = function (value, min, max) { return Math.min(max, Math.max(min, value)); };

    var showFallback = function () {
        stage.classList.add("no-webgl");
    };

    var supportsWebGL = function () {
        try {
            var testCanvas = document.createElement("canvas");
            return !!(window.WebGLRenderingContext && (testCanvas.getContext("webgl") || testCanvas.getContext("experimental-webgl")));
        } catch (e) {
            return false;
        }
    };

    // --- Canvas-texture helpers: every crate is a real "batch" with a printed label ---
    var shadeColor = function (hex, percent) {
        var t = percent < 0 ? 0 : 255;
        var p = Math.abs(percent) / 100;
        var r = (hex >> 16) & 0xff, g = (hex >> 8) & 0xff, b = hex & 0xff;
        var nr = Math.round((t - r) * p) + r;
        var ng = Math.round((t - g) * p) + g;
        var nb = Math.round((t - b) * p) + b;
        return (nr << 16) + (ng << 8) + nb;
    };
    var hexToCss = function (hex) { return "#" + ("000000" + hex.toString(16)).slice(-6); };

    var roundRect = function (ctx, x, y, w, h, r) {
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.arcTo(x + w, y, x + w, y + h, r);
        ctx.arcTo(x + w, y + h, x, y + h, r);
        ctx.arcTo(x, y + h, x, y, r);
        ctx.arcTo(x, y, x + w, y, r);
        ctx.closePath();
    };

    var wrapCenteredText = function (ctx, text, centerX, centerY, maxWidth, lineHeight) {
        var words = text.split(" ");
        var lines = [];
        var line = "";
        words.forEach(function (word) {
            var test = line ? line + " " + word : word;
            if (ctx.measureText(test).width > maxWidth && line) {
                lines.push(line);
                line = word;
            } else {
                line = test;
            }
        });
        if (line) lines.push(line);
        var startY = centerY - ((lines.length - 1) * lineHeight) / 2;
        lines.forEach(function (l, i) { ctx.fillText(l, centerX, startY + i * lineHeight); });
    };

    var makeSideTexture = function (product) {
        var size = 256;
        var c = document.createElement("canvas");
        c.width = size; c.height = size;
        var ctx = c.getContext("2d");

        var grad = ctx.createLinearGradient(0, 0, size, size);
        grad.addColorStop(0, hexToCss(shadeColor(product.color, 16)));
        grad.addColorStop(1, hexToCss(shadeColor(product.color, -26)));
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, size, size);

        ctx.strokeStyle = "rgba(255,255,255,0.22)";
        ctx.lineWidth = 6;
        ctx.strokeRect(6, 6, size - 12, size - 12);

        ctx.strokeStyle = "rgba(255,255,255,0.1)";
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(0, size * 0.5); ctx.lineTo(size, size * 0.5);
        ctx.stroke();

        var plateX = size * 0.08, plateY = size * 0.33, plateW = size * 0.84, plateH = size * 0.4;
        ctx.fillStyle = "rgba(250,253,252,0.95)";
        roundRect(ctx, plateX, plateY, plateW, plateH, 10);
        ctx.fill();

        ctx.textAlign = "center";
        ctx.fillStyle = "#06343e";
        ctx.font = "800 27px Arial, sans-serif";
        wrapCenteredText(ctx, product.label, size / 2, plateY + plateH * 0.42, plateW * 0.86, 28);

        ctx.fillStyle = "#0b9185";
        ctx.font = "800 16px Arial, sans-serif";
        ctx.fillText(product.sku, size / 2, plateY + plateH * 0.84);

        var barY = size * 0.82;
        ctx.fillStyle = "rgba(255,255,255,0.42)";
        var bx = size * 0.16;
        while (bx < size * 0.84) {
            var w = 2 + Math.random() * 4;
            ctx.fillRect(bx, barY, w, size * 0.09);
            bx += w + 3 + Math.random() * 4;
        }

        if (product.lowStock) {
            ctx.save();
            ctx.translate(size * 0.86, size * 0.14);
            ctx.rotate(Math.PI / 4);
            ctx.fillStyle = "#e0862b";
            ctx.fillRect(-46, -11, 92, 22);
            ctx.fillStyle = "#fff";
            ctx.font = "800 13px Arial, sans-serif";
            ctx.textAlign = "center";
            ctx.fillText("LOW STOCK", 0, 5);
            ctx.restore();
        }

        return new THREE.CanvasTexture(c);
    };

    var makeTopTexture = function (product) {
        var size = 256;
        var c = document.createElement("canvas");
        c.width = size; c.height = size;
        var ctx = c.getContext("2d");

        ctx.fillStyle = hexToCss(shadeColor(product.color, 8));
        ctx.fillRect(0, 0, size, size);

        ctx.strokeStyle = "rgba(255,255,255,0.25)";
        ctx.lineWidth = 4;
        ctx.beginPath(); ctx.moveTo(size / 2, 12); ctx.lineTo(size / 2, size - 12); ctx.stroke();

        ctx.strokeStyle = "rgba(255,255,255,0.12)";
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(0, 0); ctx.lineTo(size * 0.28, size * 0.5);
        ctx.moveTo(size, 0); ctx.lineTo(size * 0.72, size * 0.5);
        ctx.moveTo(0, size); ctx.lineTo(size * 0.28, size * 0.5);
        ctx.moveTo(size, size); ctx.lineTo(size * 0.72, size * 0.5);
        ctx.stroke();

        ctx.fillStyle = "rgba(255,255,255,0.82)";
        ctx.font = "800 18px Arial, sans-serif";
        ctx.textAlign = "center";
        ctx.fillText("LFD", size / 2, size / 2 + 6);

        return new THREE.CanvasTexture(c);
    };

    var products = [
        { label: "MILK POWDER", name: "Milk Powder 400g", sku: "MP-4002", batch: "B-2291", qty: "48 units", location: "Rack A-3", color: 0x0b9185 },
        { label: "HOUSEHOLD", name: "Household Cleaner 1L", sku: "HC-1001", batch: "B-1187", qty: "120 units", location: "Rack B-2", color: 0x0d6b63 },
        { label: "COOKING OIL", name: "Cooking Oil 5L", sku: "CO-5003", batch: "B-0552", qty: "36 units", location: "Rack A-1", color: 0x144b52, lowStock: true },
        { label: "BASMATI RICE", name: "Basmati Rice 5kg", sku: "BR-5010", batch: "B-3387", qty: "64 units", location: "Rack C-4", color: 0x1c5a55 },
        { label: "DISH SOAP", name: "Dish Soap 750ml", sku: "DS-0750", batch: "B-2765", qty: "90 units", location: "Rack B-1", color: 0x2f8f83 },
        { label: "TEA LEAVES", name: "Tea Leaves 200g", sku: "TL-0200", batch: "B-1420", qty: "150 units", location: "Rack A-2", color: 0x0b9185 },
        { label: "LAUNDRY PWD", name: "Laundry Powder 1kg", sku: "LP-1000", batch: "B-0938", qty: "55 units", location: "Rack C-1", color: 0x0d6b63 },
        { label: "NOODLES 5PK", name: "Instant Noodles 5-Pack", sku: "IN-0500", batch: "B-3012", qty: "200 units", location: "Rack D-3", color: 0x144b52 },
        { label: "WATER 1.5L", name: "Bottled Water 1.5L", sku: "BW-1500", batch: "B-1654", qty: "300 units", location: "Rack D-1", color: 0x1c5a55 },
        { label: "SUGAR 1KG", name: "Sugar 1kg", sku: "SG-1000", batch: "B-2098", qty: "80 units", location: "Rack C-2", color: 0x2f8f83 }
    ];

    var positions = [
        [-1.6, -1, 0], [0, -1, 0.4], [1.6, -1, -0.2],
        [-0.8, 0.05, -0.6], [0.8, 0.05, 0.6], [-1.9, 0.1, 0.9], [1.9, 0.05, -0.7],
        [-0.9, 1.1, 0.2], [0.9, 1.1, -0.3], [0, 1.9, 0.1]
    ];

    var init = function () {
        if (typeof THREE === "undefined" || !supportsWebGL()) {
            showFallback();
            return;
        }

        var renderer;
        try {
            renderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: true, alpha: true });
        } catch (e) {
            showFallback();
            return;
        }

        // Everything below only ever affects this one canvas/stage; any failure here
        // falls back to the static preview instead of surfacing anywhere else on the page.

        var infocard = document.getElementById("scene3d-infocard");
        var infocardClose = document.getElementById("scene3d-infocard-close");
        var infocardName = document.getElementById("infocard-name");
        var infocardSku = document.getElementById("infocard-sku");
        var infocardBatch = document.getElementById("infocard-batch");
        var infocardQty = document.getElementById("infocard-qty");
        var infocardLocation = document.getElementById("infocard-location");
        var hint = document.getElementById("scene3d-hint");

        var scene = new THREE.Scene();
        var camera = new THREE.PerspectiveCamera(40, 1, 0.1, 100);
        camera.position.set(0, 1.55, 9.4);
        camera.lookAt(0, 0.35, 0);

        scene.add(new THREE.AmbientLight(0x9fd8d0, 0.6));
        var keyLight = new THREE.DirectionalLight(0x6ce1d3, 1.1);
        keyLight.position.set(4, 6, 5);
        scene.add(keyLight);
        var rimLight = new THREE.DirectionalLight(0x2f7ea8, 0.5);
        rimLight.position.set(-5, -2, -4);
        scene.add(rimLight);

        var group = new THREE.Group();
        scene.add(group);

        var boxGeometry = new THREE.BoxGeometry(1, 1, 1);
        var edgesGeometry = new THREE.EdgesGeometry(boxGeometry);
        var crates = [];

        positions.forEach(function (pos, index) {
            var product = products[index % products.length];
            var sideTexture = makeSideTexture(product);
            var topTexture = makeTopTexture(product);

            var sideMat = new THREE.MeshStandardMaterial({ map: sideTexture, roughness: 0.6, metalness: 0.08, emissive: 0x000000, emissiveIntensity: 0 });
            var topMat = new THREE.MeshStandardMaterial({ map: topTexture, roughness: 0.5, metalness: 0.05, emissive: 0x000000, emissiveIntensity: 0 });
            var bottomMat = new THREE.MeshStandardMaterial({ color: shadeColor(product.color, -40), roughness: 0.75, metalness: 0.04 });

            var box = new THREE.Mesh(boxGeometry, [sideMat, sideMat, topMat, bottomMat, sideMat, sideMat]);
            box.position.set(pos[0], pos[1], pos[2]);
            var spinBaseY = (index % 4) * 0.32;
            box.rotation.y = spinBaseY;

            var edgeColor = product.lowStock ? 0xf0a94a : 0x9be7de;
            var edges = new THREE.LineSegments(edgesGeometry, new THREE.LineBasicMaterial({ color: edgeColor, transparent: true, opacity: 0.22 }));
            box.add(edges);

            group.add(box);

            var outward = new THREE.Vector3(pos[0], 0, pos[2]);
            if (outward.lengthSq() < 0.0001) outward.set(0, 0, 1); else outward.normalize();

            crates.push({
                mesh: box,
                edges: edges,
                sideMat: sideMat,
                topMat: topMat,
                basePosition: new THREE.Vector3(pos[0], pos[1], pos[2]),
                targetPosition: new THREE.Vector3(pos[0], pos[1], pos[2]),
                spinBaseY: spinBaseY,
                outward: outward,
                baseY: pos[1],
                phase: Math.random() * Math.PI * 2,
                baseScale: 0.92,
                targetScale: 0.92,
                currentScale: 0.92,
                hovered: false,
                data: product
            });
            box.scale.setScalar(0.92);
        });

        var raycaster = new THREE.Raycaster();
        var selectedCrate = null;
        var hoveredCrate = null;
        var hasInteracted = false;

        var applyHighlight = function (crate) {
            var color = 0x000000;
            var intensity = 0;
            if (crate === selectedCrate) { color = 0x39e6cf; intensity = 0.65; }
            else if (crate === hoveredCrate) { color = 0x6ce1d3; intensity = 0.3; }
            crate.sideMat.emissive.setHex(color);
            crate.sideMat.emissiveIntensity = intensity;
            crate.topMat.emissive.setHex(color);
            crate.topMat.emissiveIntensity = intensity;
        };

        var refreshHighlights = function () { crates.forEach(applyHighlight); };

        var showInfoCard = function (data) {
            if (!infocard) return;
            infocardName.textContent = data.name;
            infocardName.parentNode.querySelectorAll(".infocard-flag").forEach(function (n) { n.remove(); });
            if (data.lowStock) {
                var flag = document.createElement("span");
                flag.className = "infocard-flag";
                flag.textContent = "Low stock — reorder soon";
                infocardName.insertAdjacentElement("afterend", flag);
            }
            infocardSku.textContent = data.sku;
            infocardBatch.textContent = data.batch;
            infocardQty.textContent = data.qty;
            infocardLocation.textContent = data.location;
            infocard.classList.add("is-active");
        };

        var hideInfoCard = function () {
            if (infocard) infocard.classList.remove("is-active");
        };

        var EJECT_LIFT = 0.85, EJECT_OUT = 0.55, EJECT_FORWARD = 1.7;

        var selectCrate = function (crate) {
            selectedCrate = crate;
            crates.forEach(function (c) {
                if (c === crate) {
                    c.targetScale = c.baseScale * 1.32;
                    c.targetPosition.set(
                        c.basePosition.x + c.outward.x * EJECT_OUT,
                        c.basePosition.y + EJECT_LIFT,
                        c.basePosition.z + c.outward.z * EJECT_OUT + EJECT_FORWARD
                    );
                } else {
                    c.targetScale = c.baseScale * 0.94;
                    c.targetPosition.copy(c.basePosition);
                }
            });
            refreshHighlights();
            showInfoCard(crate.data);
        };

        var deselectAll = function () {
            selectedCrate = null;
            crates.forEach(function (c) {
                c.targetScale = c.baseScale;
                c.targetPosition.copy(c.basePosition);
            });
            refreshHighlights();
            hideInfoCard();
        };

        if (infocardClose) {
            infocardClose.addEventListener("click", function () { deselectAll(); });
        }

        var targetRotation = { x: -0.15, y: 0.4 };
        var currentRotation = { x: -0.15, y: 0.4 };
        var isRotating = false;
        var dragStart = { x: 0, y: 0 };
        var dragStartRotation = { x: 0, y: 0 };
        var pointerDownPos = { x: 0, y: 0 };
        var hasMovedEnough = false;
        var autoDemoTimer = 0;

        var resize = function () {
            var width = stage.clientWidth;
            var height = stage.clientHeight;
            if (!width || !height) return;
            renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
            renderer.setSize(width, height, false);
            camera.aspect = width / height;
            camera.updateProjectionMatrix();
        };

        var handleTap = function (clientX, clientY) {
            try {
                var rect = canvas.getBoundingClientRect();
                if (!rect.width || !rect.height) return;
                var ndc = new THREE.Vector2(
                    ((clientX - rect.left) / rect.width) * 2 - 1,
                    -((clientY - rect.top) / rect.height) * 2 + 1
                );
                raycaster.setFromCamera(ndc, camera);
                var meshes = crates.map(function (c) { return c.mesh; });
                var hits = raycaster.intersectObjects(meshes);
                if (hits.length) {
                    var hitMesh = hits[0].object;
                    var hitCrate = null;
                    crates.forEach(function (c) { if (c.mesh === hitMesh) hitCrate = c; });
                    if (hitCrate) selectCrate(hitCrate);
                } else {
                    deselectAll();
                }
            } catch (err) {
                if (window.console && console.error) console.error("[scene3d.js] tap ignored:", err);
            }
        };

        var pointerDown = function (event) {
            isRotating = true;
            hasMovedEnough = false;
            if (!hasInteracted) {
                hasInteracted = true;
                window.clearTimeout(autoDemoTimer);
                if (hint) hint.style.opacity = "";
            }
            stage.classList.add("is-interacting");
            canvas.style.cursor = "grabbing";
            var point = event.touches ? event.touches[0] : event;
            dragStart.x = point.clientX; dragStart.y = point.clientY;
            pointerDownPos.x = point.clientX; pointerDownPos.y = point.clientY;
            dragStartRotation.x = targetRotation.x; dragStartRotation.y = targetRotation.y;
        };

        var pointerMove = function (event) {
            if (!isRotating) return;
            var point = event.touches ? event.touches[0] : event;
            var deltaX = point.clientX - dragStart.x;
            var deltaY = point.clientY - dragStart.y;
            if (Math.abs(point.clientX - pointerDownPos.x) > 6 || Math.abs(point.clientY - pointerDownPos.y) > 6) {
                hasMovedEnough = true;
            }
            targetRotation.y = dragStartRotation.y + deltaX * 0.008;
            targetRotation.x = clamp(dragStartRotation.x + deltaY * 0.006, -0.9, 0.5);
            if (event.cancelable) event.preventDefault();
        };

        var pointerUp = function (event) {
            if (!isRotating) return;
            isRotating = false;
            canvas.style.cursor = hoveredCrate ? "pointer" : "grab";
            if (!hasMovedEnough) {
                var point = (event.changedTouches && event.changedTouches[0]) || event;
                handleTap(point.clientX, point.clientY);
            }
        };

        var updateHover = function (clientX, clientY) {
            try {
                var rect = canvas.getBoundingClientRect();
                if (!rect.width || !rect.height) return;
                var ndc = new THREE.Vector2(
                    ((clientX - rect.left) / rect.width) * 2 - 1,
                    -((clientY - rect.top) / rect.height) * 2 + 1
                );
                raycaster.setFromCamera(ndc, camera);
                var meshes = crates.map(function (c) { return c.mesh; });
                var hits = raycaster.intersectObjects(meshes);
                var nextHover = null;
                if (hits.length) {
                    var hitMesh = hits[0].object;
                    crates.forEach(function (c) { if (c.mesh === hitMesh) nextHover = c; });
                }
                if (nextHover !== hoveredCrate) {
                    hoveredCrate = nextHover;
                    refreshHighlights();
                }
                canvas.style.cursor = nextHover ? "pointer" : "grab";
            } catch (err) {}
        };

        canvas.addEventListener("mousedown", pointerDown);
        window.addEventListener("mousemove", pointerMove);
        canvas.addEventListener("mousemove", function (event) {
            if (isRotating) return;
            updateHover(event.clientX, event.clientY);
        });
        canvas.addEventListener("mouseleave", function () {
            if (hoveredCrate) { hoveredCrate = null; refreshHighlights(); }
            canvas.style.cursor = "grab";
        });
        window.addEventListener("mouseup", pointerUp);
        canvas.addEventListener("touchstart", pointerDown, { passive: true });
        canvas.addEventListener("touchmove", pointerMove, { passive: false });
        window.addEventListener("touchend", pointerUp);

        var isVisible = true;
        if ("IntersectionObserver" in window) {
            var visibilityObserver = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) { isVisible = entry.isIntersecting; });
            }, { threshold: 0.05 });
            visibilityObserver.observe(stage);
        }

        autoDemoTimer = window.setTimeout(function () {
            if (!hasInteracted && isVisible) {
                selectCrate(crates[Math.floor(Math.random() * crates.length)]);
            }
        }, 1700);

        var clock = new THREE.Clock();

        var animate = function () {
            window.requestAnimationFrame(animate);
            if (!isVisible) return;
            try {
                var elapsed = clock.getElapsedTime();

                if (!reducedMotion && !isRotating && !selectedCrate) {
                    targetRotation.y += 0.0018;
                }

                currentRotation.x += (targetRotation.x - currentRotation.x) * 0.08;
                currentRotation.y += (targetRotation.y - currentRotation.y) * 0.08;
                group.rotation.x = currentRotation.x;
                group.rotation.y = currentRotation.y;

                crates.forEach(function (crate) {
                    var isSelected = crate === selectedCrate;

                    crate.mesh.position.x += (crate.targetPosition.x - crate.mesh.position.x) * 0.12;
                    crate.mesh.position.z += (crate.targetPosition.z - crate.mesh.position.z) * 0.12;
                    var floatY = (!reducedMotion && !isSelected) ? Math.sin(elapsed * 0.8 + crate.phase) * 0.05 : 0;
                    var restY = crate.targetPosition.y + floatY;
                    crate.mesh.position.y += (restY - crate.mesh.position.y) * 0.12;

                    if (isSelected) {
                        crate.mesh.rotation.y += (-currentRotation.y - crate.mesh.rotation.y) * 0.14;
                        crate.mesh.rotation.x += (-currentRotation.x - crate.mesh.rotation.x) * 0.14;
                    } else {
                        crate.mesh.rotation.y += (crate.spinBaseY - crate.mesh.rotation.y) * 0.14;
                        crate.mesh.rotation.x += (0 - crate.mesh.rotation.x) * 0.14;
                    }

                    crate.currentScale += (crate.targetScale - crate.currentScale) * 0.15;
                    crate.mesh.scale.setScalar(crate.currentScale);

                    if (crate.data.lowStock && !isSelected && !reducedMotion) {
                        crate.edges.material.opacity = 0.24 + Math.sin(elapsed * 2.2) * 0.16;
                    } else if (crate.data.lowStock) {
                        crate.edges.material.opacity = 0.24;
                    }
                });

                renderer.render(scene, camera);
            } catch (err) {
                if (window.console && console.error) console.error("[scene3d.js] render frame skipped:", err);
            }
        };

        window.addEventListener("resize", resize);
        resize();
        animate();
    };

    var safeInit = function () {
        try {
            init();
        } catch (err) {
            if (window.console && console.error) console.error("[scene3d.js] falling back to static preview:", err);
            showFallback();
        }
    };

    // Runs after the whole page (and every other script) has already finished loading,
    // so this 3D extra can never delay or interfere with anything else on the page.
    if (document.readyState === "complete") {
        window.setTimeout(safeInit, 0);
    } else {
        window.addEventListener("load", () => window.setTimeout(safeInit, 0), { once: true });
    }
})();