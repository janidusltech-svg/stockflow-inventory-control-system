document.addEventListener("DOMContentLoaded", () => {
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const clamp = (value, min = 0, max = 1) => Math.min(max, Math.max(min, value));

    // Small helper so a bug in one feature can never take the rest of the page down with it.
    const safe = (label, fn) => {
        try {
            fn();
        } catch (err) {
            if (window.console && console.error) console.error("[landing.js] " + label + " failed:", err);
        }
    };

    // --- Safety net: no matter what else happens, never leave the loader stuck or content hidden ---
    const forceRevealEverything = () => {
        document.querySelectorAll(".reveal").forEach((item) => item.classList.add("visible"));
        const loader = document.getElementById("site-loader");
        if (loader && !loader.hasAttribute("hidden")) {
            loader.classList.add("is-finished");
            loader.setAttribute("hidden", "");
        }
        document.body.classList.remove("loading");
    };
    window.setTimeout(forceRevealEverything, 6500);

    // --- Theme toggle ---
    safe("theme toggle", () => {
        const themeToggle = document.querySelector("#theme-toggle");
        const metaThemeColor = document.querySelector('meta[name="theme-color"]');
        const applyTheme = (theme, persist) => {
            if (theme === "dark") document.documentElement.setAttribute("data-theme", "dark");
            else document.documentElement.removeAttribute("data-theme");
            if (themeToggle) {
                themeToggle.setAttribute("aria-pressed", String(theme === "dark"));
                themeToggle.setAttribute("aria-label", theme === "dark" ? "Switch to light theme" : "Switch to dark theme");
            }
            if (metaThemeColor) metaThemeColor.setAttribute("content", theme === "dark" ? "#05181d" : "#062d3a");
            if (persist) {
                try { localStorage.setItem("stockflow-theme", theme); } catch (e) {}
            }
        };
        applyTheme(document.documentElement.getAttribute("data-theme") === "dark" ? "dark" : "light", false);
        if (themeToggle) {
            themeToggle.addEventListener("click", () => {
                const isDark = document.documentElement.getAttribute("data-theme") === "dark";
                applyTheme(isDark ? "light" : "dark", true);
            });
        }
    });

    // --- Loader ---
    safe("loader", () => {
        const loader = document.querySelector("#site-loader");
        let loaderFinished = false;
        const finishLoader = () => {
            if (loaderFinished) return;
            loaderFinished = true;
            if (loader) loader.classList.add("is-finished");
            document.body.classList.remove("loading");
            window.setTimeout(() => { if (loader) loader.setAttribute("hidden", ""); }, 650);
        };
        const scheduleLoaderFinish = () => window.setTimeout(finishLoader, reducedMotion ? 120 : 2550);
        if (document.readyState === "complete") scheduleLoaderFinish();
        else window.addEventListener("load", scheduleLoaderFinish, { once: true });
        window.setTimeout(finishLoader, reducedMotion ? 500 : 4500);
    });

    // --- Header + mobile nav ---
    safe("header/nav", () => {
        const header = document.querySelector(".landing-header");
        const menuButton = document.querySelector(".menu-toggle");
        const navigation = document.querySelector(".landing-nav");

        const updateHeader = () => header && header.classList.toggle("scrolled", window.scrollY > 14);
        updateHeader();
        window.addEventListener("scroll", updateHeader, { passive: true });

        if (menuButton) {
            menuButton.addEventListener("click", () => {
                const expanded = menuButton.getAttribute("aria-expanded") === "true";
                menuButton.setAttribute("aria-expanded", String(!expanded));
                if (navigation) navigation.classList.toggle("open", !expanded);
                document.body.classList.toggle("menu-open", !expanded);
            });
        }

        if (navigation) {
            navigation.querySelectorAll("a").forEach((link) => {
                link.addEventListener("click", () => {
                    if (menuButton) menuButton.setAttribute("aria-expanded", "false");
                    navigation.classList.remove("open");
                    document.body.classList.remove("menu-open");
                });
            });
        }
    });

    // --- Reveal-on-scroll ---
    safe("reveal", () => {
        const revealItems = document.querySelectorAll(".reveal");
        if (!reducedMotion && "IntersectionObserver" in window) {
            const revealObserver = new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add("visible");
                        revealObserver.unobserve(entry.target);
                    }
                });
            }, { threshold: 0.1, rootMargin: "0px 0px -45px" });
            revealItems.forEach((item) => revealObserver.observe(item));
        } else {
            revealItems.forEach((item) => item.classList.add("visible"));
        }
    });

    // --- Hero scroll-driven scene ---
    safe("hero motion", () => {
        const heroSection = document.querySelector(".hero-section");
        const heroCart = document.querySelector("#hero-worker-cart");
        const heroDataPath = document.querySelector(".hero-data-path");
        const heroProgressPath = document.querySelector("#hero-progress-path");
        const heroDataDot = document.querySelector(".hero-data-dot");
        const heroFlowItems = Array.from(document.querySelectorAll("[data-hero-flow]"));
        const heroSceneStops = Array.from(document.querySelectorAll("[data-hero-stop]"));
        const heroFlowStatus = document.querySelector("#hero-flow-status");
        const heroStatuses = [
            "Supplier delivery received",
            "Batch and expiry details captured",
            "Storage location assigned",
            "Available stock saved to MySQL"
        ];
        let heroActiveStep = -1;
        let heroDataLength = 0;
        let heroProgressLength = 0;
        let heroTargetProgress = 0;
        let heroRenderedProgress = 0;
        let heroAnimationFrame = 0;

        const setHeroStep = (step) => {
            const nextStep = Math.round(clamp(step, 0, 3));
            if (nextStep === heroActiveStep) return;
            heroActiveStep = nextStep;
            heroFlowItems.forEach((item, index) => item.classList.toggle("active", index === nextStep));
            heroSceneStops.forEach((item, index) => {
                item.classList.toggle("active", index === nextStep);
                item.classList.toggle("complete", index < nextStep);
            });
            if (heroFlowStatus) heroFlowStatus.textContent = heroStatuses[nextStep];
            if (heroSection) heroSection.setAttribute("data-active-stage", String(nextStep));
        };

        const renderHeroMotion = (progress) => {
            if (!heroSection || !heroCart) return;
            heroSection.style.setProperty("--hero-progress", progress.toFixed(4));
            heroCart.setAttribute("transform", `translate(${progress * 1080} 0)`);
            const step = Math.min(3, Math.max(0, Math.round(progress * 3)));
            setHeroStep(step);

            if (heroDataPath && heroDataDot && heroDataLength) {
                try {
                    const point = heroDataPath.getPointAtLength(heroDataLength * progress);
                    heroDataDot.setAttribute("cx", String(point.x));
                    heroDataDot.setAttribute("cy", String(point.y));
                } catch (e) {}
            }

            if (heroProgressPath && heroProgressLength) {
                heroProgressPath.style.strokeDashoffset = `${heroProgressLength * (1 - progress)}`;
            }
        };

        const measureHeroProgress = () => {
            if (!heroSection) return 0;
            if (reducedMotion || window.innerWidth <= 620) return 0.5;
            const sectionTop = heroSection.offsetTop;
            const travelDistance = Math.max(1, heroSection.offsetHeight - window.innerHeight);
            return clamp((window.scrollY - sectionTop) / travelDistance);
        };

        const animateHeroMotion = () => {
            const difference = heroTargetProgress - heroRenderedProgress;
            heroRenderedProgress += difference * 0.095;

            if (Math.abs(difference) < 0.0005) {
                heroRenderedProgress = heroTargetProgress;
                heroAnimationFrame = 0;
            }

            renderHeroMotion(heroRenderedProgress);
            if (heroAnimationFrame) heroAnimationFrame = window.requestAnimationFrame(animateHeroMotion);
        };

        const requestHeroUpdate = () => {
            heroTargetProgress = measureHeroProgress();
            if (reducedMotion) {
                heroRenderedProgress = heroTargetProgress;
                renderHeroMotion(heroRenderedProgress);
                return;
            }
            if (!heroAnimationFrame) heroAnimationFrame = window.requestAnimationFrame(animateHeroMotion);
        };

        if (heroSection && heroCart) {
            if (heroDataPath) {
                try { heroDataLength = heroDataPath.getTotalLength(); } catch (e) { heroDataLength = 0; }
            }
            if (heroProgressPath) {
                try {
                    heroProgressLength = heroProgressPath.getTotalLength();
                    heroProgressPath.style.strokeDasharray = `${heroProgressLength}`;
                    heroProgressPath.style.strokeDashoffset = `${heroProgressLength}`;
                } catch (e) { heroProgressLength = 0; }
            }
            heroTargetProgress = measureHeroProgress();
            heroRenderedProgress = heroTargetProgress;
            renderHeroMotion(heroRenderedProgress);
            window.addEventListener("scroll", requestHeroUpdate, { passive: true });
            window.addEventListener("resize", requestHeroUpdate);
        }
    });

    // --- Journey scroll-driven scene ---
    safe("journey motion", () => {
        const journeySection = document.querySelector(".journey-section");
        const journeyRoad = document.querySelector("#journey-road-path");
        const journeyProgressRoad = document.querySelector("#journey-road-progress");
        const truck = document.querySelector("#journey-truck");
        const stopsContainer = document.querySelector("#journey-stops");
        const chapters = Array.from(document.querySelectorAll(".journey-chapter"));
        const currentNumber = document.querySelector("#journey-current-number");
        const statusText = document.querySelector("#journey-status");
        const outputText = document.querySelector("#journey-output");
        const stopProgress = [0, 0.2, 0.4, 0.6, 0.8, 1];
        let roadLength = 0;
        let stopElements = [];
        let activeStep = 0;
        let journeyTargetProgress = 0;
        let journeyRenderedProgress = 0;
        let journeyAnimationFrame = 0;

        const setActiveStep = (stepIndex) => {
            const nextStep = clamp(stepIndex, 0, chapters.length - 1);
            activeStep = nextStep;
            chapters.forEach((chapter, index) => chapter.classList.toggle("active", index === nextStep));
            stopElements.forEach((stop, index) => {
                stop.classList.toggle("active", index === nextStep);
                stop.classList.toggle("complete", index < nextStep);
            });
            const chapter = chapters[nextStep];
            if (chapter) {
                if (currentNumber) currentNumber.textContent = String(nextStep + 1).padStart(2, "0");
                if (statusText) statusText.textContent = chapter.dataset.status;
                if (outputText) outputText.textContent = chapter.dataset.output;
            }
        };

        const placeStops = () => {
            if (!journeyRoad || !stopsContainer || !roadLength) return;
            stopsContainer.innerHTML = "";
            stopElements = stopProgress.map((progress, index) => {
                let point = { x: 0, y: 0 };
                try { point = journeyRoad.getPointAtLength(roadLength * progress); } catch (e) {}
                const stop = document.createElement("span");
                stop.className = "journey-stop";
                stop.textContent = String(index + 1).padStart(2, "0");
                stop.style.left = `${(point.x / 520) * 100}%`;
                stop.style.top = `${(point.y / 720) * 100}%`;
                stopsContainer.appendChild(stop);
                return stop;
            });
            setActiveStep(activeStep);
        };

        const renderJourney = (progress) => {
            if (!journeySection || !journeyRoad || !truck || !roadLength) return;
            journeySection.style.setProperty("--journey-bg-up", `${-progress * 175}px`);
            journeySection.style.setProperty("--journey-bg-down", `${progress * 145}px`);
            journeySection.style.setProperty("--journey-grid-y", `${progress * 230}px`);
            journeySection.style.setProperty("--journey-glow-y", `${8 + progress * 84}%`);

            let point = { x: 0, y: 0 };
            let angle = 0;
            try {
                const distance = roadLength * progress;
                point = journeyRoad.getPointAtLength(distance);
                const ahead = journeyRoad.getPointAtLength(Math.min(roadLength, distance + 2));
                angle = Math.atan2(ahead.y - point.y, ahead.x - point.x) * (180 / Math.PI) - 90;
            } catch (e) {}

            truck.style.left = `${(point.x / 520) * 100}%`;
            truck.style.top = `${(point.y / 720) * 100}%`;
            truck.style.transform = `translate(-50%, -50%) rotate(${angle}deg)`;

            if (journeyProgressRoad) {
                journeyProgressRoad.style.strokeDasharray = `${roadLength}`;
                journeyProgressRoad.style.strokeDashoffset = `${roadLength * (1 - progress)}`;
            }

            const calculatedStep = Math.min(5, Math.max(0, Math.round(progress * 5)));
            if (calculatedStep !== activeStep) setActiveStep(calculatedStep);
        };

        const measureJourneyProgress = () => {
            if (!journeySection) return 0;
            if (reducedMotion) return stopProgress[activeStep];
            const sectionBounds = journeySection.getBoundingClientRect();
            const sectionStart = window.scrollY + sectionBounds.top;
            const sectionEnd = sectionStart + journeySection.offsetHeight - window.innerHeight;
            return clamp((window.scrollY - sectionStart) / Math.max(1, sectionEnd - sectionStart));
        };

        const animateJourney = () => {
            const difference = journeyTargetProgress - journeyRenderedProgress;
            journeyRenderedProgress += difference * 0.11;

            if (Math.abs(difference) < 0.0005) {
                journeyRenderedProgress = journeyTargetProgress;
                journeyAnimationFrame = 0;
            }

            renderJourney(journeyRenderedProgress);
            if (journeyAnimationFrame) journeyAnimationFrame = window.requestAnimationFrame(animateJourney);
        };

        const requestJourneyUpdate = () => {
            journeyTargetProgress = measureJourneyProgress();
            if (reducedMotion) {
                journeyRenderedProgress = journeyTargetProgress;
                renderJourney(journeyRenderedProgress);
                return;
            }
            if (!journeyAnimationFrame) journeyAnimationFrame = window.requestAnimationFrame(animateJourney);
        };

        if (journeyRoad && chapters.length) {
            try { roadLength = journeyRoad.getTotalLength(); } catch (e) { roadLength = 0; }
            if (journeyProgressRoad && roadLength) {
                journeyProgressRoad.style.strokeDasharray = `${roadLength}`;
                journeyProgressRoad.style.strokeDashoffset = `${roadLength}`;
            }
            placeStops();
            setActiveStep(0);
            journeyTargetProgress = measureJourneyProgress();
            journeyRenderedProgress = journeyTargetProgress;
            renderJourney(journeyRenderedProgress);
            window.addEventListener("scroll", requestJourneyUpdate, { passive: true });
            window.addEventListener("resize", () => {
                placeStops();
                requestJourneyUpdate();
            });
        }
    });

    // --- Animated stat counters ---
    safe("counters", () => {
        const countElements = document.querySelectorAll("[data-count]");
        if (!reducedMotion && "IntersectionObserver" in window) {
            const countObserver = new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    if (!entry.isIntersecting) return;
                    const element = entry.target;
                    const target = Number(element.dataset.count);
                    const startedAt = performance.now();
                    const duration = 700;
                    const animate = (time) => {
                        const progress = clamp((time - startedAt) / duration);
                        element.textContent = String(Math.max(1, Math.round(target * progress)));
                        if (progress < 1) window.requestAnimationFrame(animate);
                    };
                    window.requestAnimationFrame(animate);
                    countObserver.unobserve(element);
                });
            }, { threshold: 0.7 });
            countElements.forEach((element) => countObserver.observe(element));
        }
    });

    // --- Hero live inventory pulse (illustrative preview data, not a real feed) ---
    safe("live metrics", () => {
        const liveMetrics = Array.from(document.querySelectorAll("[data-live-metric]"));
        if (liveMetrics.length && !reducedMotion) {
            const bumpLiveMetric = () => {
                const element = liveMetrics[Math.floor(Math.random() * liveMetrics.length)];
                const key = element.dataset.liveMetric;
                const current = Number(element.dataset.liveBase);
                let next = current;
                if (key === "items") {
                    next = current + (Math.random() > 0.25 ? 1 : -1);
                } else {
                    const delta = Math.random() > 0.5 ? 1 : -1;
                    next = Math.max(0, Math.min(25, current + delta));
                }
                element.dataset.liveBase = String(next);
                element.textContent = next.toLocaleString();
                element.classList.add("pulse");
                window.setTimeout(() => element.classList.remove("pulse"), 500);
            };
            window.setInterval(bumpLiveMetric, 3200);
        }
    });

    // --- Magnetic pull on primary call-to-action buttons ---
    safe("magnetic buttons", () => {
        const magneticElements = Array.from(document.querySelectorAll(".magnetic"));
        if (magneticElements.length && !reducedMotion && window.matchMedia("(pointer: fine)").matches) {
            magneticElements.forEach((element) => {
                const resetTransform = () => { element.style.transform = ""; };
                element.addEventListener("mousemove", (event) => {
                    const bounds = element.getBoundingClientRect();
                    const relativeX = event.clientX - (bounds.left + bounds.width / 2);
                    const relativeY = event.clientY - (bounds.top + bounds.height / 2);
                    const pullX = clamp(relativeX * 0.22, -7, 7);
                    const pullY = clamp(relativeY * 0.28, -6, 6);
                    element.style.transform = `translate(${pullX}px, ${pullY - 2}px)`;
                });
                element.addEventListener("mouseleave", resetTransform);
                element.addEventListener("blur", resetTransform);
            });
        }
    });

    // --- Hero role-perspective word cycling ---
    safe("hero role cycle", () => {
        const roleWord = document.querySelector("#hero-role-cycle");
        if (!roleWord) return;
        const roles = ["Store Supervisors", "Finance Executives", "Operations Managers", "Customer Service", "Systems Teams"];
        let roleIndex = 0;
        if (reducedMotion) return;
        window.setInterval(() => {
            roleIndex = (roleIndex + 1) % roles.length;
            roleWord.classList.add("is-swapping");
            window.setTimeout(() => {
                roleWord.textContent = roles[roleIndex];
                roleWord.classList.remove("is-swapping");
            }, 280);
        }, 2800);
    });

    // --- Sitewide autoplay tour: scrolls the whole page top-to-bottom at a natural pace ---
    safe("autoplay tour", () => {
        const toggle = document.querySelector("#autoplay-toggle");
        if (!toggle) return;
        if (reducedMotion) { toggle.remove(); return; }

        const PIXELS_PER_SECOND = 270;
        const label = toggle.querySelector("span");
        let playing = false;
        let rafId = 0;
        let lastTimestamp = 0;

        const stopAutoplay = () => {
            if (!playing) return;
            playing = false;
            toggle.classList.remove("is-playing");
            toggle.setAttribute("aria-pressed", "false");
            if (label) label.textContent = "Autoplay tour";
            if (rafId) window.cancelAnimationFrame(rafId);
            rafId = 0;
            window.removeEventListener("wheel", onInterrupt);
            window.removeEventListener("touchstart", onInterrupt);
            window.removeEventListener("keydown", onKeyInterrupt);
        };

        function onInterrupt() { stopAutoplay(); }
        function onKeyInterrupt(event) {
            const interruptKeys = ["ArrowUp", "ArrowDown", "PageUp", "PageDown", "Home", "End", " "];
            if (interruptKeys.indexOf(event.key) !== -1) stopAutoplay();
        }

        const tick = (time) => {
            if (!playing) return;
            if (!lastTimestamp) lastTimestamp = time;
            const deltaSeconds = (time - lastTimestamp) / 1000;
            lastTimestamp = time;
            const maxScroll = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
            const next = Math.min(maxScroll, window.scrollY + PIXELS_PER_SECOND * deltaSeconds);
            window.scrollTo(0, next);
            if (next >= maxScroll - 1) {
                stopAutoplay();
                return;
            }
            rafId = window.requestAnimationFrame(tick);
        };

        const startAutoplay = () => {
            playing = true;
            lastTimestamp = 0;
            toggle.classList.add("is-playing");
            toggle.setAttribute("aria-pressed", "true");
            if (label) label.textContent = "Stop autoplay";
            window.addEventListener("wheel", onInterrupt, { passive: true });
            window.addEventListener("touchstart", onInterrupt, { passive: true });
            window.addEventListener("keydown", onKeyInterrupt);
            window.scrollTo(0, 0);
            rafId = window.requestAnimationFrame(tick);
        };

        toggle.addEventListener("click", () => {
            if (playing) stopAutoplay();
            else startAutoplay();
        });
    });

    // --- Cursor-tracked spotlight glow on card surfaces ---
    safe("spotlight cards", () => {
        const spotlightCards = Array.from(document.querySelectorAll(".spotlight-card"));
        if (spotlightCards.length && !reducedMotion && window.matchMedia("(pointer: fine)").matches) {
            spotlightCards.forEach((card) => {
                card.addEventListener("mousemove", (event) => {
                    const bounds = card.getBoundingClientRect();
                    card.style.setProperty("--spot-x", `${((event.clientX - bounds.left) / bounds.width) * 100}%`);
                    card.style.setProperty("--spot-y", `${((event.clientY - bounds.top) / bounds.height) * 100}%`);
                });
            });
        }
    });
});