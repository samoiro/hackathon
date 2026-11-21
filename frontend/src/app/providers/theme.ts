import { theme } from 'antd'
import type { ThemeConfig } from 'antd'

// Morning Yellow 디자인 시스템. 색 값은 src/styles/index.css @theme과 같다
const lightColors = {
  colorPrimary: '#fe9c00',
  colorPrimaryHover: '#ed9000',
  colorPrimaryActive: '#d47d00',
  colorPrimaryBg: '#fff6e5',
  colorPrimaryBgHover: '#ffe8b8',
  // 흰 배경 위 brand 글자는 2.1:1이라 brand-text 갈색(9.5:1)을 쓴다
  colorPrimaryText: '#693a00',
  colorLink: '#693a00',

  colorBgLayout: '#faf8f4',
  colorBgContainer: '#ffffff',
  colorBgElevated: '#ffffff',
  colorText: '#241a0d',
  colorTextSecondary: '#64594b',
  colorBorder: '#897b6a',
  colorBorderSecondary: '#e5ded3',

  // 상태색은 브랜드가이드 04 서울 브랜드 네 색의 글자색·배경 (옅은 배경 위 5:1 이상)
  colorSuccess: '#007b45',
  colorSuccessBg: '#e0fff1',
  colorWarning: '#8a6100',
  colorWarningBg: '#fff6e0',
  colorError: '#c20053',
  colorErrorBg: '#ffe0ee',
  colorInfo: '#0063c2',
  colorInfoBg: '#e0f0ff',
}

// index.css :root[data-theme='dark'] 와 같은 값
const darkColors = {
  ...lightColors,
  colorPrimaryBg: '#332a0c',
  colorPrimaryBgHover: '#4a3d12',
  colorPrimaryText: '#ffc15c',
  colorLink: '#ffc15c',

  colorBgLayout: '#0c0c0d',
  colorBgContainer: '#202022',
  colorBgElevated: '#2a2a2d',
  colorText: '#e8e8ea',
  colorTextSecondary: '#b4b4b9',
  colorBorder: '#8f8f96',
  colorBorderSecondary: '#3a3a3f',

  colorSuccess: '#4fe3a1',
  colorSuccessBg: '#0f3324',
  colorWarning: '#ffcb57',
  colorWarningBg: '#332a0c',
  colorError: '#ff7ab0',
  colorErrorBg: '#3f0f24',
  colorInfo: '#7abdff',
  colorInfoBg: '#0f2a44',
}

export const FONT_SCALE = { normal: 1, large: 1.125, xlarge: 1.25 } as const

// 글자 크기 설정은 글자와 함께 버튼·입력 높이도 같은 비율로 키운다
export function getAppTheme(dark: boolean, scale: number): ThemeConfig {
  const px = (value: number) => Math.round(value * scale)
  const brandText = dark ? '#ffc15c' : '#693a00'

  return {
    algorithm: dark ? theme.darkAlgorithm : theme.defaultAlgorithm,
    token: {
      ...(dark ? darkColors : lightColors),

      fontFamily:
        '"Pretendard Variable", Pretendard, -apple-system, BlinkMacSystemFont, system-ui, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif',
      fontSize: px(16),
      fontSizeSM: px(14),
      fontSizeHeading1: px(32),
      fontSizeHeading2: px(24),
      lineHeight: 26 / 16,
      fontWeightStrong: 600,

      // 높이 규칙: 버튼·입력 48, 작은 버튼 36, 큰 버튼 56
      controlHeight: px(48),
      controlHeightSM: px(36),
      controlHeightLG: px(56),
      // 모서리 규칙: 입력·버튼 12, 카드 16, 태그·체크박스 8
      borderRadius: 12,
      borderRadiusLG: 16,
      borderRadiusSM: 8,
    },
    components: {
      // 버튼 체계(헤더·버튼 시안): 주 버튼은 노란 면 + 본문색 글자(8.1:1), 보조 버튼은 진한 테두리, 글자는 SemiBold
      Button: {
        primaryColor: '#241a0d',
        fontWeight: 600,
        // 작은 버튼(S·36)은 글자 14
        contentFontSizeSM: px(14),
        defaultBorderColor: dark ? '#e8e8ea' : '#241a0d',
        defaultHoverBorderColor: dark ? '#e8e8ea' : '#241a0d',
        defaultHoverColor: dark ? '#e8e8ea' : '#241a0d',
        defaultHoverBg: dark ? '#2c2c30' : '#f5f1ea',
        defaultActiveBorderColor: dark ? '#e8e8ea' : '#241a0d',
        defaultActiveColor: dark ? '#e8e8ea' : '#241a0d',
        defaultActiveBg: dark ? '#3a3a3f' : '#e9e2d6',
        textHoverBg: dark ? '#2c2c30' : '#f5f1ea',
      },
      // 팝업 모서리 20
      Modal: {
        borderRadiusLG: 20,
        titleFontSize: px(24),
        titleLineHeight: 34 / 24,
      },
      // 태그는 꼬리표 12/18, 테두리까지 높이 24
      // 기본 태그는 중립색(리뷰 F-01: 분홍 배경은 오류 전용이라 '필수'도 중립)
      Tag: {
        fontSizeSM: px(12),
        lineHeightSM: 22 / 12,
        defaultBg: dark ? '#2c2c30' : '#f5f1ea',
        defaultColor: dark ? '#b4b4b9' : '#64594b',
      },
      // 기본값은 글자·밑줄 모두 colorPrimary(흰 배경 2.1:1). 글자는 brand-text, 밑줄은 primary-700(3.1:1)
      Tabs: {
        inkBarColor: '#d47d00',
        itemSelectedColor: brandText,
        itemHoverColor: brandText,
        itemActiveColor: brandText,
      },
      // 체크박스·라디오는 선택 표시를 본문색으로(Figma). 주황은 화면당 한 곳에만 쓴다
      Checkbox: {
        colorPrimary: dark ? '#e8e8ea' : '#241a0d',
        colorPrimaryHover: dark ? '#e8e8ea' : '#241a0d',
        colorWhite: dark ? '#241a0d' : '#ffffff',
      },
      Radio: {
        colorPrimary: dark ? '#e8e8ea' : '#241a0d',
        colorPrimaryHover: dark ? '#e8e8ea' : '#241a0d',
        colorWhite: dark ? '#241a0d' : '#ffffff',
      },
      Switch: { colorPrimary: '#d47d00', colorPrimaryHover: '#d47d00' },
    },
  }
}
