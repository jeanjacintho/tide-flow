
export const Colors = {

  primary: '#7DD87D',
  primaryForeground: '#FCF9F0',

  background: '#FFFFFF',
  backgroundSecondary: '#F5F5F5',
  card: '#FFFFFF',

  foreground: '#1A1A1A',
  cardForeground: '#1A1A1A',
  mutedForeground: '#6B7280',

  secondary: '#F7F7F7',
  secondaryForeground: '#2A2A2A',

  border: '#EBEBEB',
  input: '#EBEBEB',
  muted: '#F7F7F7',

  destructive: '#E63946',
  success: '#7DD87D',
  warning: '#F59E0B',
  error: '#E63946',

  userMessage: '#7DD87D',
  assistantMessage: '#FFFFFF',
  userMessageText: '#FFFFFF',
  assistantMessageText: '#1A1A1A',

  placeholder: '#999999',

  loading: '#7DD87D',

  shadow: '#000000',

  overlay: 'rgba(0, 0, 0, 0.5)',
} as const;

export const Radius = {
  sm: 8,
  md: 10,
  lg: 12,
  xl: 16,
  full: 9999,
} as const;

export const Spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 40,
} as const;

export const Typography = {
  sizes: {
    xs: 12,
    sm: 14,
    base: 16,
    lg: 18,
    xl: 20,
    '2xl': 24,
    '3xl': 32,
  },
  weights: {
    normal: '400' as const,
    medium: '500' as const,
    semibold: '600' as const,
    bold: '700' as const,
  },
} as const;
