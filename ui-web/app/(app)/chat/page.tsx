'use client';

import { useState, useTransition, useEffect, useRef, useCallback } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadConversationHistory = useCallback(async () => {
    if (!conversationId || !user?.id) return;

    try {
      const history = await apiService.getConversationHistory(conversationId, user.id);
      setMessages(history.messages);
      setShowExamples(false);
    } catch (error) {
      console.error('Error loading conversation history:', error);
      // Se a conversa não existir mais, limpa o localStorage
      if (error instanceof Error && error.message.includes('not found')) {
        localStorage.removeItem(CONVERSATION_ID_KEY);
        setConversationId(undefined);
      }
    }
  }, [conversationId, user]);

  // Salva conversationId no localStorage quando ele muda
  useEffect(() => {
    if (conversationId) {
      localStorage.setItem(CONVERSATION_ID_KEY, conversationId);
    }
  }, [conversationId]);

  // Recupera conversationId do localStorage ao montar (se houver)
  // O sistema de memória já mantém o contexto, então só precisamos carregar as mensagens visuais
  useEffect(() => {
    if (!user?.id) return;

    const savedConversationId = localStorage.getItem(CONVERSATION_ID_KEY);
    if (savedConversationId && !conversationId) {
      // Usa setTimeout para evitar setState síncrono no effect
      setTimeout(() => {
        setConversationId(savedConversationId);
      }, 0);
    }
  }, [user?.id, conversationId]);

  useEffect(() => {
    // Aguarda um pouco para a animação de expansão acontecer
    const timer = setTimeout(() => {
      scrollToBottom();
    }, 100);
    return () => clearTimeout(timer);
  }, [messages]);

  // Efeito para calcular opacidade baseada no scroll
  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;

    const handleScroll = () => {
      const scrollTop = container.scrollTop;
      const scrollHeight = container.scrollHeight;
      const clientHeight = container.clientHeight;
      
      // Só aplica o efeito se houver scroll (conteúdo maior que o container)
      const hasScroll = scrollHeight > clientHeight;
      
      if (!hasScroll) {
        // Se não há scroll, todas as mensagens ficam opacas
        messageRefs.current.forEach((element) => {
          if (element) {
            element.style.opacity = '1';
          }
        });
        return;
      }

      // Atualiza opacidade de cada mensagem baseada na posição
      messageRefs.current.forEach((element) => {
        if (!element) return;
        
        const rect = element.getBoundingClientRect();
        const containerRect = container.getBoundingClientRect();
        
        // Calcula a posição relativa da mensagem no container
        const relativeTop = rect.top - containerRect.top;
        const containerHeight = containerRect.height;
        const messageHeight = rect.height;
        const messageBottom = relativeTop + messageHeight;
        
        // Zona de fade (150px do topo e do fundo)
        const fadeZone = 150;
        
        let opacity = 1;
        
        // Fade no topo: apenas quando há scroll para baixo e mensagem está saindo pelo topo
        if (scrollTop > 0 && relativeTop < fadeZone) {
          // Mensagem está saindo pelo topo
          opacity = Math.max(0.3, relativeTop / fadeZone);
        }
        // Fade no fundo: apenas quando não está no final do scroll e mensagem está saindo pelo fundo
        else if (scrollTop + clientHeight < scrollHeight - 10 && messageBottom > containerHeight - fadeZone) {
          // Mensagem está saindo pelo fundo
          const distanceFromBottom = containerHeight - messageBottom;
          opacity = Math.max(0.3, distanceFromBottom / fadeZone);
        }
        
        element.style.opacity = opacity.toString();
      });
    };

    container.addEventListener('scroll', handleScroll);
    // Usa ResizeObserver para detectar mudanças no tamanho do conteúdo
    const resizeObserver = new ResizeObserver(() => {
      handleScroll();
    });
    resizeObserver.observe(container);
    
    // Chama uma vez para calcular opacidade inicial
    handleScroll();

    return () => {
      container.removeEventListener('scroll', handleScroll);
      resizeObserver.disconnect();
    };
  }, [messages]);

  useEffect(() => {
    if (conversationId && user?.id) {
      // Usa setTimeout para evitar chamada síncrona de setState
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

    // Adiciona mensagem do usuário imediatamente
    const userMessage: Message = {
      id: `temp-${Date.now()}`,
      role: 'USER',
      content: currentMessage,
      createdAt: new Date().toISOString(),
      sequenceNumber: messages.length + 1,
    };
    setMessages(prev => [...prev, userMessage]);

    // Mostra spinner da IA imediatamente
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
          // O useEffect acima já salva no localStorage automaticamente

          // Adiciona resposta da IA
          const aiMessage: Message = {
            id: `temp-ai-${Date.now()}`,
            role: 'ASSISTANT',
            content: response.response,
            createdAt: new Date().toISOString(),
            sequenceNumber: messages.length + 2,
          };
          setMessages(prev => [...prev, aiMessage]);

          // Recarrega histórico completo para ter IDs corretos
          if (response.conversationId) {
            await loadConversationHistory();
          }
        } catch (error) {
          console.error('Error sending message:', error);
          setMessage(currentMessage);
          // Remove mensagem temporária do usuário em caso de erro
          setMessages(prev => prev.filter(msg => msg.id !== userMessage.id));
          alert(error instanceof Error ? error.message : 'Erro ao enviar mensagem. Tente novamente.');
        } finally {
          // Remove spinner da IA
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
    <div className="flex h-full flex-col bg-gray-100 dark:bg-gray-900 overflow-hidden">
      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden relative min-h-0">
        {messages.length === 0 && showExamples ? (
          // Initial state - centered layout with input
          <div className="flex-1 flex flex-col items-center justify-center px-6 py-20">
            <div className="w-full max-w-5xl space-y-12">
              {/* Title Section */}
              <div className="text-left space-y-3">
                <h2 className="text-5xl font-bold bg-gradient-to-r from-zinc-800 via-primary to-purple-500 bg-clip-text text-transparent">
                  Olá, {user?.name || 'usuário'}
                </h2>
                <p className="text-5xl font-medium bg-gradient-to-r from-zinc-800 via-primary to-purple-500 bg-clip-text text-transparent">
                  Como posso ajudar você hoje?
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-4">
                  Use uma das perguntas mais comuns abaixo ou escreva sua própria para começar.
                </p>
              </div>

              {/* Example Cards - Horizontal Layout */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {EXAMPLE_QUERIES.map((example, index) => (
                  <button
                    key={index}
                    onClick={() => handleExampleClick(example)}
                    className="flex flex-col items-start p-5 text-left bg-white dark:bg-gray-800 rounded-lg transition-all duration-200 group border border-gray-200 dark:border-gray-700"
                  >
                    <span className="text-sm text-gray-700 dark:text-gray-300 mb-3 leading-relaxed">{example}</span>
                    <div className="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center mt-auto group-hover:bg-gray-200 dark:group-hover:bg-gray-600 transition-colors">
                      <ChevronRight className="w-4 h-4 text-gray-500 dark:text-gray-400" />
                    </div>
                  </button>
                ))}
              </div>

              {/* Input Area - Centered with examples */}
              <div className="w-full">
                <form onSubmit={handleSubmit}>
                  <div className="relative flex items-center bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-4">
                    {/* Input */}
                    <Input
                      type="text"
                      placeholder="Pergunte qualquer coisa..."
                      value={message}
                      onChange={(e) => setMessage(e.target.value)}
                      disabled={isPending || isAiThinking}
                      className="flex-1 border-0 shadow-none focus-visible:ring-0 text-base bg-transparent placeholder:text-gray-400 dark:placeholder:text-gray-500 py-2"
                    />

                    {/* Right side - Character count and send button */}
                    <div className="flex items-center gap-3 ml-3">
                      <span className="text-xs text-gray-400 dark:text-gray-500">
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
                        className="h-9 w-9 bg-primary text-white rounded-lg"
                      >
                        <ArrowUp className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </form>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col overflow-hidden relative min-h-0">
            {/* Chat messages - animated expansion with fade effect */}
            <div 
              ref={messagesContainerRef}
              className="flex-1 overflow-y-auto px-6 py-8 pb-32 transition-all duration-500 ease-in-out min-h-0"
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
                            ? 'bg-primary border border-[color-mix(in_srgb,theme(colors.primary),black_10%)] dark:bg-gray-200 text-white dark:text-gray-900 rounded-md'
                            : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100 rounded-md'
                        )}
                      >
                        <p className="text-sm leading-relaxed whitespace-pre-wrap break-words">{msg.content}</p>
                      </div>
                      <span
                        className={cn(
                          "text-[10px] mt-0.5 px-1 select-none",
                          msg.role === 'USER'
                            ? 'text-gray-400 dark:text-gray-400'
                            : 'text-gray-400 dark:text-gray-500'
                        )}
                      >
                        {formatMessageTime(msg.createdAt)}
                      </span>
                    </div>
                  </div>
                ))}
                {isAiThinking && (
                  <div
                    className={cn(
                      "flex w-full animate-in fade-in slide-in-from-bottom-2 duration-300 justify-start"
                    )}
                  >
                    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100 rounded-md px-5 py-4">
                      <div className="flex items-center gap-2">
                        <div className="inline-block w-4 h-4 border-2 border-gray-400 dark:border-gray-500 border-t-transparent rounded-full animate-spin"></div>
                        <span className="text-sm text-gray-500 dark:text-gray-400">Pensando...</span>
                      </div>
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            </div>

            {/* Input Area - Fixed at bottom when chat is active */}
            <div className="absolute bottom-0 left-0 right-0 z-10 w-full bg-gray-100 dark:bg-gray-900 pb-6 pt-4 transition-all duration-500 ease-in-out border-t border-gray-200 dark:border-gray-800">
              <div className="max-w-4xl mx-auto px-6">
                <form onSubmit={handleSubmit}>
                  <div className="relative flex items-center bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-4">
                    {/* Input */}
                    <Input
                      type="text"
                      placeholder="Pergunte qualquer coisa..."
                      value={message}
                      onChange={(e) => setMessage(e.target.value)}
                      disabled={isPending || isAiThinking}
                      className="flex-1 border-0 shadow-none focus-visible:ring-0 text-base bg-transparent placeholder:text-gray-400 dark:placeholder:text-gray-500 py-2"
                    />

                    {/* Right side - Character count and send button */}
                    <div className="flex items-center gap-3 ml-3">
                      <span className="text-xs text-gray-400 dark:text-gray-500">
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
                        className="h-9 w-9 bg-primary text-white rounded-full"
                      >
                        <ArrowUp className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
