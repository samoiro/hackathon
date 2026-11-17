import type { ThemeConfig } from 'antd'

// Morning Yellow 디자인 시스템. 색 값은 src/styles/index.css @theme과 같다
export const appTheme: ThemeConfig = {
  token: {
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
    colorText: '#241a0d',
    colorTextSecondary: '#64594b',
    colorBorder: '#897b6a',
    colorBorderSecondary: '#e5ded3',

    // 성공은 2025 서울색 Green Aurora 계열
    colorSuccess: '#00695c',
    colorSuccessBg: '#dcf5ec',
    colorWarning: '#754700',
    colorWarningBg: '#fff1cf',
    colorError: '#a42635',
    colorErrorBg: '#fdecef',
    colorInfo: '#235789',
    colorInfoBg: '#eaf3ff',

    fontFamily:
      '"Pretendard Variable", Pretendard, -apple-system, BlinkMacSystemFont, system-ui, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif',
    fontSize: 16,
    fontSizeSM: 14,
    fontSizeHeading1: 32,
    fontSizeHeading2: 24,
    lineHeight: 26 / 16,
    fontWeightStrong: 700,

    controlHeight: 44,
    borderRadius: 8,
    borderRadiusLG: 16,
  },
  components: {
    // brand 배경 위 흰 글자는 2.1:1이라 본문색(8.1:1)을 쓴다
    Button: { primaryColor: '#241a0d' },
    // 기본값은 글자·밑줄 모두 colorPrimary(흰 배경 2.1:1). 글자는 brand-text(9.5:1), 밑줄은 primary-700(3.1:1)
    Tabs: {
      inkBarColor: '#d47d00',
      itemSelectedColor: '#693a00',
      itemHoverColor: '#693a00',
      itemActiveColor: '#693a00',
    },
    // 채운 상자·트랙이 흰 배경과 3:1이 되도록 primary-700, 그 위 체크·점은 본문색(5.5:1). colorWhite는 체크·점 색으로만 쓰인다
    Checkbox: {
      colorPrimary: '#d47d00',
      colorPrimaryHover: '#d47d00',
      colorWhite: '#241a0d',
    },
    Radio: {
      colorPrimary: '#d47d00',
      colorPrimaryHover: '#d47d00',
      colorWhite: '#241a0d',
    },
    Switch: { colorPrimary: '#d47d00', colorPrimaryHover: '#d47d00' },
  },
}
