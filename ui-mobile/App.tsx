import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { ChatScreen } from './screens/ChatScreen';
import { LoginScreen } from './screens/LoginScreen';
import { useAuth } from './hooks/useAuth';
import { Colors } from './constants/colors';

function AppContent() {
  const { isAuthenticated, isLoading } = useAuth();

  // Durante o carregamento inicial, mostra loading
  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  // Primeira tela sempre é LoginScreen se não estiver autenticado
  // Só vai para ChatScreen se o usuário já estiver logado
  if (!isAuthenticated) {
    return <LoginScreen />;
  }

  return <ChatScreen />;
}

export default function App() {
  return (
    <SafeAreaProvider>
      <AppContent />
      <StatusBar style="dark" />
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.background,
  },
});
