// Cores do projeto TideFlow - alinhadas com ui-web
// Baseadas em oklch para consistência

// Função auxiliar para converter oklch para valores que React Native pode usar
// React Native não suporta oklch diretamente, então usamos valores hex equivalentes

export const Colors = {
  // Cores primárias
  primary: '#7DD87D', // oklch(86.06% 0.1766 134.52) - verde primário
  primaryForeground: '#FCF9F0', // oklch(0.986 0.031 120.757)
  
  // Cores de fundo
  background: '#FFFFFF', // oklch(1 0 0) - branco
  backgroundSecondary: '#F5F5F5', // cinza muito claro
  card: '#FFFFFF', // oklch(1 0 0)
  
  // Cores de texto
  foreground: '#1A1A1A', // oklch(0.141 0.005 285.823) - preto escuro
  cardForeground: '#1A1A1A', // oklch(0.141 0.005 285.823)
  mutedForeground: '#6B7280', // oklch(0.552 0.016 285.938) - cinza médio
  
  // Cores secundárias
  secondary: '#F7F7F7', // oklch(0.967 0.001 286.375)
  secondaryForeground: '#2A2A2A', // oklch(0.21 0.006 285.885)
  
  // Cores de borda e input
  border: '#EBEBEB', // oklch(0.92 0.004 286.32)
  input: '#EBEBEB', // oklch(0.92 0.004 286.32)
  muted: '#F7F7F7', // oklch(0.967 0.001 286.375)
  
  // Cores de estado
  destructive: '#E63946', // oklch(0.577 0.245 27.325) - vermelho
  success: '#7DD87D', // mesma cor do primary para sucesso
  warning: '#F59E0B', // amarelo/laranja
  error: '#E63946', // mesma cor do destructive
  
  // Cores de chat (específicas para mensagens)
  userMessage: '#7DD87D', // primary color
  assistantMessage: '#FFFFFF',
  userMessageText: '#FFFFFF',
  assistantMessageText: '#1A1A1A',
  
  // Cores de placeholder
  placeholder: '#999999',
  
  // Cores de loading
  loading: '#7DD87D', // primary color
  
  // Cores de sombra (para iOS)
  shadow: '#000000',
  
  // Cores de overlay
  overlay: 'rgba(0, 0, 0, 0.5)',
} as const;

// Radius do projeto (0.65rem = ~10.4px, arredondado para 12px para mobile)
export const Radius = {
  sm: 8,
  md: 10,
  lg: 12,
  xl: 16,
  full: 9999,
} as const;

// Espaçamentos padrão
export const Spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 40,
} as const;

// Tipografia
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

