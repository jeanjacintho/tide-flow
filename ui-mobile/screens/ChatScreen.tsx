import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  StatusBar,
  Platform,
  KeyboardAvoidingView,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { ChatInput } from '../components/ChatInput';
import { apiService, Message } from '../services/api';
import { useAuth } from '../hooks/useAuth';
import { Colors, Radius, Spacing, Typography } from '../constants/colors';

const CONVERSATION_ID_KEY = 'tideflow_conversation_id';

export const ChatScreen: React.FC = () => {
  const { user } = useAuth();
  const [messages, setMessages] = useState<Message[]>([]);
  const [conversationId, setConversationId] = useState<string | undefined>();
  const [isSending, setIsSending] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const flatListRef = useRef<FlatList>(null);

  useEffect(() => {
    if (messages.length > 0) {
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({ animated: true });
      }, 100);
    }
  }, [messages]);

  const loadConversationHistory = useCallback(async () => {
    if (!conversationId || !user?.id) return;

    setIsLoadingHistory(true);
    try {
      const history = await apiService.getConversationHistory(conversationId, user.id);
      setMessages(history.messages || []);
    } catch (error) {
      console.error('Error loading conversation history:', error);
      if (error instanceof Error && error.message.includes('not found')) {
        await AsyncStorage.removeItem(CONVERSATION_ID_KEY);
        setConversationId(undefined);
      }
    } finally {
      setIsLoadingHistory(false);
    }
  }, [conversationId, user?.id]);

  useEffect(() => {
    if (conversationId) {
      AsyncStorage.setItem(CONVERSATION_ID_KEY, conversationId);
    }
  }, [conversationId]);

  useEffect(() => {
    if (!user?.id) return;

    const loadSavedConversationId = async () => {
      try {
        const savedConversationId = await AsyncStorage.getItem(CONVERSATION_ID_KEY);
        if (savedConversationId && !conversationId) {
          setConversationId(savedConversationId);
        }
      } catch (error) {
        console.error('Error loading saved conversation ID:', error);
      }
    };

    loadSavedConversationId();
  }, [user?.id, conversationId]);

  useEffect(() => {
    if (conversationId && user?.id) {
      loadConversationHistory();
    }
  }, [conversationId, user?.id, loadConversationHistory]);

  const handleSendMessage = async (text: string) => {
    if (!user?.id) {
      Alert.alert('Erro', 'Usuário não encontrado. Faça login novamente.');
      return;
    }

    setIsSending(true);

    const userMessage: Message = {
      id: `temp-${Date.now()}`,
      role: 'USER',
      content: text,
      createdAt: new Date().toISOString(),
      sequenceNumber: messages.length + 1,
    };

    setMessages((prev) => [...prev, userMessage]);

    try {
      const response = await apiService.sendMessage(text, conversationId, user.id);

      setConversationId(response.conversationId);

      const aiMessage: Message = {
        id: `temp-ai-${Date.now()}`,
        role: 'ASSISTANT',
        content: response.aiResponse,
        createdAt: new Date().toISOString(),
        sequenceNumber: messages.length + 2,
      };

      setMessages((prev) => [...prev, aiMessage]);

      if (response.conversationId) {
        await loadConversationHistory();
      }
    } catch (error) {
      console.error('Error sending message:', error);
      setMessages((prev) => prev.filter((msg) => msg.id !== userMessage.id));
      Alert.alert(
        'Erro',
        error instanceof Error ? error.message : 'Erro ao enviar mensagem. Tente novamente.'
      );
    } finally {
      setIsSending(false);
    }
  };

  const formatMessageTime = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  };

  const renderMessage = ({ item }: { item: Message }) => {
    const isUser = item.role === 'USER';

    return (
      <View
        style={[
          styles.messageContainer,
          isUser ? styles.userMessageContainer : styles.assistantMessageContainer,
        ]}
      >
        <View
          style={[
            styles.messageBubble,
            isUser ? styles.userMessageBubble : styles.assistantMessageBubble,
          ]}
        >
          <Text
            style={[
              styles.messageText,
              isUser ? styles.userMessageText : styles.assistantMessageText,
            ]}
          >
            {item.content}
          </Text>
        </View>
        <Text style={styles.timestamp}>
          {formatMessageTime(item.createdAt)}
        </Text>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor={Colors.background} />
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
      >
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Chat</Text>
        </View>

        {isLoadingHistory ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color={Colors.primary} />
            <Text style={styles.loadingText}>Carregando conversa...</Text>
          </View>
        ) : (
          <FlatList
            ref={flatListRef}
            data={messages}
            renderItem={renderMessage}
            keyExtractor={(item) => item.id}
            contentContainerStyle={styles.messagesList}
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyText}>
                  Olá, {user?.name || 'usuário'}!{'\n'}
                  Como posso ajudar você hoje?
                </Text>
              </View>
            }
            onContentSizeChange={() => {
              flatListRef.current?.scrollToEnd({ animated: true });
            }}
          />
        )}

        <ChatInput
          onSendMessage={handleSendMessage}
          disabled={isSending || isLoadingHistory}
        />
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  container: {
    flex: 1,
    backgroundColor: Colors.backgroundSecondary,
  },
  header: {
    backgroundColor: Colors.background,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
    ...Platform.select({
      ios: {
        paddingTop: 50,
      },
    }),
  },
  headerTitle: {
    fontSize: Typography.sizes.xl,
    fontWeight: Typography.weights.semibold,
    color: Colors.foreground,
  },
  messagesList: {
    flexGrow: 1,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.md,
  },
  messageContainer: {
    marginVertical: Spacing.xs,
    maxWidth: '80%',
  },
  userMessageContainer: {
    alignSelf: 'flex-end',
    alignItems: 'flex-end',
  },
  assistantMessageContainer: {
    alignSelf: 'flex-start',
    alignItems: 'flex-start',
  },
  messageBubble: {
    paddingHorizontal: Spacing.md,
    paddingVertical: 10,
    borderRadius: Radius.lg,
    ...Platform.select({
      ios: {
        shadowColor: Colors.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
      },
      android: {
        elevation: 2,
      },
    }),
  },
  userMessageBubble: {
    backgroundColor: Colors.userMessage,
    borderBottomRightRadius: Radius.sm,
  },
  assistantMessageBubble: {
    backgroundColor: Colors.assistantMessage,
    borderBottomLeftRadius: Radius.sm,
  },
  messageText: {
    fontSize: Typography.sizes.base,
    lineHeight: 20,
  },
  userMessageText: {
    color: Colors.userMessageText,
  },
  assistantMessageText: {
    color: Colors.assistantMessageText,
  },
  timestamp: {
    fontSize: Typography.sizes.xs,
    color: Colors.mutedForeground,
    marginTop: Spacing.xs,
    paddingHorizontal: Spacing.xs,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 60,
  },
  emptyText: {
    fontSize: Typography.sizes.base,
    color: Colors.mutedForeground,
    textAlign: 'center',
    lineHeight: 24,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 60,
  },
  loadingText: {
    marginTop: Spacing.md,
    fontSize: Typography.sizes.base,
    color: Colors.mutedForeground,
  },
});
