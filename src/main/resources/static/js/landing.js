document.addEventListener("DOMContentLoaded", () => {
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const header = document.querySelector(".landing-header");
    const menuButton = document.querySelector(".menu-toggle");
    const navigation = document.querySelector(".landing-nav");

    const clamp = (value, min = 0, max = 1) => Math.min(max, Math.max(min, value));

    const loader = document.querySelector("#site-loader");
    let loaderFinished = false;
    const finishLoader = () => {
        if (loaderFinished) return;
        loaderFinished = true;
        loader?.classList.add("is-finished");
        document.body.classList.remove("loading");
        window.setTimeout(() => loader?.setAttribute("hidden", ""), 650);
    };
    const scheduleLoaderFinish = () => window.setTimeout(finishLoader, reducedMotion ? 120 : 2550);
    if (document.readyState === "complete") scheduleLoaderFinish();
    else window.addEventListener("load", scheduleLoaderFinish, { once: true });
    window.setTimeout(finishLoader, reducedMotion ? 500 : 4500);

    const updateHeader = () => header?.classList.toggle("scrolled", window.scrollY > 14);
    updateHeader();
    window.addEventListener("scroll", updateHeader, { passive: true });

    menuButton?.addEventListener("click", () => {
        const expanded = menuButton.getAttribute("aria-expanded") === "true";
        menuButton.setAttribute("aria-expanded", String(!expanded));
        navigation?.classList.toggle("open", !expanded);
        document.body.classList.toggle("menu-open", !expanded);
    });

    navigation?.querySelectorAll("a").forEach((link) => {
        link.addEventListener("click", () => {
            menuButton?.setAttribute("aria-expanded", "false");
            navigation.classList.remove("open");
            document.body.classList.remove("menu-open");
        });
    });

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
    };

    const renderHeroMotion = (progress) => {
        if (!heroSection || !heroCart) return;
        heroSection.style.setProperty("--hero-progress", progress.toFixed(4));
        heroCart.setAttribute("transform", `translate(${progress * 1080} 0)`);
        const step = Math.min(3, Math.max(0, Math.round(progress * 3)));
        setHeroStep(step);

        if (heroDataPath && heroDataDot && heroDataLength) {
            const point = heroDataPath.getPointAtLength(heroDataLength * progress);
            heroDataDot.setAttribute("cx", String(point.x));
            heroDataDot.setAttribute("cy", String(point.y));
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
        if (heroDataPath) heroDataLength = heroDataPath.getTotalLength();
        if (heroProgressPath) {
            heroProgressLength = heroProgressPath.getTotalLength();
            heroProgressPath.style.strokeDasharray = `${heroProgressLength}`;
            heroProgressPath.style.strokeDashoffset = `${heroProgressLength}`;
        }
        heroTargetProgress = measureHeroProgress();
        heroRenderedProgress = heroTargetProgress;
        renderHeroMotion(heroRenderedProgress);
        window.addEventListener("scroll", requestHeroUpdate, { passive: true });
        window.addEventListener("resize", requestHeroUpdate);
    }

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
            currentNumber.textContent = String(nextStep + 1).padStart(2, "0");
            statusText.textContent = chapter.dataset.status;
            outputText.textContent = chapter.dataset.output;
        }
    };

    const placeStops = () => {
        if (!journeyRoad || !stopsContainer || !roadLength) return;
        stopsContainer.innerHTML = "";
        stopElements = stopProgress.map((progress, index) => {
            const point = journeyRoad.getPointAtLength(roadLength * progress);
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
        const distance = roadLength * progress;
        const point = journeyRoad.getPointAtLength(distance);
        const ahead = journeyRoad.getPointAtLength(Math.min(roadLength, distance + 2));
        const angle = Math.atan2(ahead.y - point.y, ahead.x - point.x) * (180 / Math.PI) - 90;

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
        roadLength = journeyRoad.getTotalLength();
        if (journeyProgressRoad) {
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
