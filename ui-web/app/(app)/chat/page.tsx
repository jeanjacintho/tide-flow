'use client';

import { useState, useTransition, useEffect, useRef, useCallback } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { apiService, ConversationResponse, Message } from "@/lib/api";
import { ArrowUp, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";
import SpeechInput from "@/components/speetch-input";

const EXAMPLE_QUERIES = [
  "Hoje estou triste",
  "Hoje estou ansioso",
  "Hoje estou com medo",
  "Hoje estou com raiva"
];

const CONVERSATION_ID_KEY = 'tideflow_conversation_id';

const formatMessageTime = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const time = `${hours}:${minutes}`;

  if (messageDate.getTime() === today.getTime()) {
    return time;
  } else if (messageDate.getTime() === yesterday.getTime()) {
    return `Ontem às ${time}`;
  } else {
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    const currentYear = now.getFullYear();

    if (year === currentYear) {
      return `${day}/${month} às ${time}`;
    } else {
      return `${day}/${month}/${year} às ${time}`;
    }
  }
};

export default function Chat() {
  const { user } = useAuth();
  const [message, setMessage] = useState('');
  const [conversationId, setConversationId] = useState<string | undefined>();
  const [messages, setMessages] = useState<Message[]>([]);
  const [isPending, startTransition] = useTransition();
  const [isAiThinking, setIsAiThinking] = useState(false);
  const [showExamples, setShowExamples] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const messageRefs = useRef<Map<string, HTMLDivElement>>(new Map());

  const scrollToBottom = () => {
    if (messagesContainerRef.current) {
      messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
    } else {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const loadConversationHistory = useCallback(async () => {
    if (!conversationId || !user?.id) return;

    try {
      const history = await apiService.getConversationHistory(conversationId, user.id);
      setMessages(history.messages);
      setShowExamples(false);
    } catch (error) {
      console.error('Erro ao carregar histórico da conversa:', error);

      if (error instanceof Error && error.message.includes('not found')) {
        localStorage.removeItem(CONVERSATION_ID_KEY);
        setConversationId(undefined);
      }
    }
  }, [conversationId, user]);

  useEffect(() => {
    if (conversationId) {
      localStorage.setItem(CONVERSATION_ID_KEY, conversationId);
    }
  }, [conversationId]);

  useEffect(() => {
    if (!user?.id) return;

    const savedConversationId = localStorage.getItem(CONVERSATION_ID_KEY);
    if (savedConversationId && !conversationId) {

      setTimeout(() => {
        setConversationId(savedConversationId);
      }, 0);
    }
  }, [user?.id, conversationId]);

  useEffect(() => {

    const timer = setTimeout(() => {
      scrollToBottom();
    }, 100);
    return () => clearTimeout(timer);
  }, [messages]);

  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;

    const handleScroll = () => {
      const scrollTop = container.scrollTop;
      const scrollHeight = container.scrollHeight;
      const clientHeight = container.clientHeight;

      const hasScroll = scrollHeight > clientHeight;

      if (!hasScroll) {

        messageRefs.current.forEach((element) => {
          if (element) {
            element.style.opacity = '1';
          }
        });
        return;
      }

      messageRefs.current.forEach((element) => {
        if (!element) return;

        const rect = element.getBoundingClientRect();
        const containerRect = container.getBoundingClientRect();

        const relativeTop = rect.top - containerRect.top;
        const containerHeight = containerRect.height;
        const messageHeight = rect.height;
        const messageBottom = relativeTop + messageHeight;

        const fadeZone = 150;

        let opacity = 1;

        if (scrollTop > 0 && relativeTop < fadeZone) {

          opacity = Math.max(0.3, relativeTop / fadeZone);
        }

        else if (scrollTop + clientHeight < scrollHeight - 10 && messageBottom > containerHeight - fadeZone) {

          const distanceFromBottom = containerHeight - messageBottom;
          opacity = Math.max(0.3, distanceFromBottom / fadeZone);
        }

        element.style.opacity = opacity.toString();
      });
    };

    container.addEventListener('scroll', handleScroll);

    const resizeObserver = new ResizeObserver(() => {
      handleScroll();
    });
    resizeObserver.observe(container);

    handleScroll();

    return () => {
      container.removeEventListener('scroll', handleScroll);
      resizeObserver.disconnect();
    };
  }, [messages]);

  useEffect(() => {
    if (conversationId && user?.id) {

      const timer = setTimeout(() => {
        loadConversationHistory();
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [conversationId, user?.id, loadConversationHistory]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim() || isPending || isAiThinking) return;

    const currentMessage = message;
    setMessage('');
    setShowExamples(false);

    if (!user?.id) {
      console.error('User ID não encontrado');
      return;
    }

    const userMessage: Message = {
      id: `temp-${Date.now()}`,
      role: 'USER',
      content: currentMessage,
      createdAt: new Date().toISOString(),
      sequenceNumber: messages.length + 1,
    };
    setMessages(prev => [...prev, userMessage]);

    setIsAiThinking(true);

    startTransition(() => {
      (async () => {
        try {
          const response: ConversationResponse = await apiService.sendMessage(
            currentMessage,
            conversationId,
            user.id
          );

          setConversationId(response.conversationId);

          const aiMessage: Message = {
            id: `temp-ai-${Date.now()}`,
            role: 'ASSISTANT',
            content: response.response,
            createdAt: new Date().toISOString(),
            sequenceNumber: messages.length + 2,
          };
          setMessages(prev => [...prev, aiMessage]);

          if (response.conversationId) {
            await loadConversationHistory();
          }
        } catch (error) {
          console.error('Erro ao enviar mensagem:', error);
          setMessage(currentMessage);

          setMessages(prev => prev.filter(msg => msg.id !== userMessage.id));
          alert(error instanceof Error ? error.message : 'Erro ao enviar mensagem. Tente novamente.');
        } finally {

          setIsAiThinking(false);
        }
      })();
    });
  };

  const handleExampleClick = (example: string) => {
    setMessage(example);
    setShowExamples(false);
  };

  return (
    <div className="h-full flex flex-col overflow-hidden">
      {messages.length === 0 && showExamples ? (

        <div className="flex-1 flex items-center justify-center p-6">
          <div className="w-full max-w-5xl space-y-6">
            {}
            <div className="text-left">
              <h1 className="text-3xl font-bold">Chat</h1>
              <p className="text-muted-foreground mt-1">
                Converse com o assistente de bem-estar emocional
              </p>
            </div>

            {}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {EXAMPLE_QUERIES.map((example, index) => (
                <Card
                  key={index}
                  onClick={() => handleExampleClick(example)}
                  className="flex flex-col items-start cursor-pointer transition-all duration-200 group hover:bg-accent h-full"
                >
                  <div className="w-full flex flex-col h-full">
                    <span className="text-sm text-foreground mb-3 leading-relaxed">{example}</span>
                    <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center mt-auto group-hover:bg-muted/80 transition-colors">
                      <ChevronRight className="w-4 h-4 text-muted-foreground" />
                    </div>
                  </div>
                </Card>
              ))}
            </div>

            {}
            <form onSubmit={handleSubmit}>
              <div className="relative flex items-center bg-card border border-border rounded-xl p-4">
                <Input
                  type="text"
                  placeholder="Pergunte qualquer coisa..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  disabled={isPending || isAiThinking}
                  className="flex-1 border-0 shadow-none focus-visible:ring-0 text-base bg-transparent placeholder:text-muted-foreground py-2"
                />
                <div className="flex items-center gap-3 ml-3">
                  <span className="text-xs text-muted-foreground">
                    {message.length}/1000
                  </span>
                  <SpeechInput
                    onTranscriptChange={(transcript) => setMessage(transcript)}
                    disabled={isPending || isAiThinking}
                    conversationId={conversationId}
                    userId={user?.id}
                  />
                  <Button
                    type="submit"
                    size="icon"
                    disabled={!message.trim() || isPending || isAiThinking}
                    className="h-9 w-9 bg-primary text-primary-foreground rounded-lg"
                  >
                    <ArrowUp className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </form>
          </div>
        </div>
      ) : (

        <div className="h-full flex flex-col overflow-hidden">
          {}
          <div className="flex-shrink-0 p-6 pb-4 border-b border-border">
            <h1 className="text-3xl font-bold">Chat</h1>
            <p className="text-muted-foreground mt-1">
              Converse com o assistente de bem-estar emocional
            </p>
          </div>

          {}
          <div
            ref={messagesContainerRef}
            className="flex-1 overflow-y-auto px-6 py-6 min-h-0"
          >
            <div className="max-w-4xl mx-auto space-y-6">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  ref={(el) => {
                    if (el) {
                      messageRefs.current.set(msg.id, el);
                    } else {
                      messageRefs.current.delete(msg.id);
                    }
                  }}
                  className={cn(
                    "flex w-full animate-in fade-in slide-in-from-bottom-2 duration-300 transition-opacity",
                    msg.role === 'USER' ? 'justify-end' : 'justify-start'
                  )}
                  style={{ opacity: 1 }}
                >
                  <div className={cn(
                    "flex flex-col max-w-[75%]",
                    msg.role === 'USER' ? 'items-end' : 'items-start'
                  )}>
                    <div
                      className={cn(
                        "px-5 py-4",
                        msg.role === 'USER'
                          ? 'bg-primary text-primary-foreground rounded-md'
                          : 'bg-card border border-border text-foreground rounded-md'
                      )}
                    >
                      <p className="text-sm leading-relaxed whitespace-pre-wrap break-words">{msg.content}</p>
                    </div>
                    <span
                      className={cn(
                        "text-[10px] mt-0.5 px-1 select-none text-muted-foreground"
                      )}
                    >
                      {formatMessageTime(msg.createdAt)}
                    </span>
                  </div>
                </div>
              ))}
              {isAiThinking && (
                <div className="flex w-full animate-in fade-in slide-in-from-bottom-2 duration-300 justify-start">
                  <div className="bg-card border border-border text-foreground rounded-md px-5 py-4">
                    <div className="flex items-center gap-2">
                      <div className="inline-block w-4 h-4 border-2 border-muted-foreground border-t-transparent rounded-full animate-spin"></div>
                      <span className="text-sm text-muted-foreground">Pensando...</span>
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
          </div>

          {}
          <div className="flex-shrink-0 p-6 pt-4 bg-background">
            <form onSubmit={handleSubmit}>
              <div className="relative flex items-center bg-card border border-border rounded-xl p-4 max-w-4xl mx-auto">
                <Input
                  type="text"
                  placeholder="Pergunte qualquer coisa..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  disabled={isPending || isAiThinking}
                  className="flex-1 border-0 shadow-none focus-visible:ring-0 text-base bg-transparent placeholder:text-muted-foreground py-2"
                />
                <div className="flex items-center gap-3 ml-3">
                  <span className="text-xs text-muted-foreground">
                    {message.length}/1000
                  </span>
                  <SpeechInput
                    onTranscriptChange={(transcript) => setMessage(transcript)}
                    disabled={isPending || isAiThinking}
                    conversationId={conversationId}
                    userId={user?.id}
                  />
                  <Button
                    type="submit"
                    size="icon"
                    disabled={!message.trim() || isPending || isAiThinking}
                    className="h-9 w-9 bg-primary text-primary-foreground rounded-lg"
                  >
                    <ArrowUp className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
