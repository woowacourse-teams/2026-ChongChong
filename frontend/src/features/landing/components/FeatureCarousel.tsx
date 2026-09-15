import { previewFeatures } from '../previewFeatures';
import { TRANSITION_DURATION, type FeaturePreview } from '../useFeaturePreview';
import FeatureScreen from './FeatureScreen';
import { FEATURE_CAROUSEL_STYLES } from './FeatureCarousel.styles';
import { FEATURE_SCREEN_STYLES } from './FeatureScreen.styles';

const slides = [
  previewFeatures[previewFeatures.length - 1],
  ...previewFeatures,
  previewFeatures[0],
];

export default function FeatureCarousel({ preview }: { preview: FeaturePreview }) {
  const { slide, activeIndex, reducedMotion, selectFeature, showcaseRef, feature } = preview;

  return (
    <section
      ref={showcaseRef}
      className="cc-feature-showcase"
      aria-label="총총 핵심 기능 미리보기"
      aria-roledescription="캐러셀"
      onFocusCapture={() => preview.setFocused(true)}
      onBlurCapture={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) preview.setFocused(false);
      }}
    >
      <style>
        {FEATURE_CAROUSEL_STYLES}
        {FEATURE_SCREEN_STYLES}
      </style>
      <div className="cc-feature-phone-stage">
        <div className="cc-feature-phone-size">
          <div
            className="cc-feature-phone"
            role="img"
            aria-label={`${feature.label} 예시 화면`}
            onPointerEnter={(event) => {
              if (event.pointerType === 'mouse') preview.setHovered(true);
            }}
            onPointerLeave={(event) => {
              if (event.pointerType === 'mouse') preview.setHovered(false);
            }}
          >
            <div className="cc-feature-phone-screen" aria-hidden="true">
              <div className="cc-feature-status">
                <span>9:41</span>
                <span className="cc-feature-island" />
                <svg viewBox="0 0 54 16" fill="currentColor">
                  <path d="M1 11h3v4H1zm5-3h3v7H6zm5-3h3v10h-3zm5-4h3v14h-3z" />
                  <path d="M23 6a9 9 0 0 1 12 0l-1.5 1.5a7 7 0 0 0-9 0zm3 3a5 5 0 0 1 6 0l-3 4z" />
                  <rect
                    x="39"
                    y="3"
                    width="12"
                    height="10"
                    rx="2"
                    fill="none"
                    stroke="currentColor"
                  />
                  <rect x="41" y="5" width="8" height="6" rx="1" />
                  <path d="M52 6h2v4h-2z" />
                </svg>
              </div>
              <div className="cc-feature-viewport">
                <div
                  className="cc-feature-track"
                  style={{
                    transform: `translateX(-${slide.position * 100}%)`,
                    transition:
                      slide.moving && !reducedMotion
                        ? `transform ${TRANSITION_DURATION}ms cubic-bezier(.22, .68, 0, 1)`
                        : 'none',
                  }}
                  onTransitionEnd={(event) => {
                    if (
                      event.target === event.currentTarget &&
                      event.propertyName === 'transform'
                    ) {
                      preview.settleTransition();
                    }
                  }}
                >
                  {slides.map((screen, index) => (
                    <div className="cc-feature-slide" key={`${screen.id}-${index}`}>
                      <FeatureScreen
                        feature={screen.id}
                        active={index === slide.position}
                        animationPaused={!preview.playing}
                      />
                    </div>
                  ))}
                </div>
              </div>
              <div className="cc-feature-home-indicator">
                <span />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="cc-feature-dots" aria-label="기능 화면 선택">
        {previewFeatures.map((feature, index) => (
          <button
            key={feature.id}
            type="button"
            className="cc-feature-dot"
            aria-label={`${feature.label} 보기`}
            aria-current={index === activeIndex ? 'step' : undefined}
            title={feature.label}
            disabled={slide.moving}
            onClick={() => selectFeature(index)}
            onKeyDown={(event) => {
              const next = {
                ArrowLeft: activeIndex - 1,
                ArrowRight: activeIndex + 1,
                Home: 0,
                End: previewFeatures.length - 1,
              }[event.key];
              if (next === undefined) return;
              event.preventDefault();
              selectFeature(next);
            }}
          >
            <span />
          </button>
        ))}
      </div>
    </section>
  );
}
