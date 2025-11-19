'use client';

import { useState, useTransition, useEffect, useRef } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { apiService, ConversationResponse, Message } from "@/lib/api";
import { ArrowUp, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

const EXAMPLE_QUERIES = [
  "Como posso melhorar minha saúde mental?",
  "Explique o que é ansiedade de forma simples",
  "Quais são os sinais de estresse?",
  "Como desenvolver resiliência emocional?"
];

export default function Chat() {
  const { user, logout } = useAuth();
  const [message, setMessage] = useState('');
  const [conversationId, setConversationId] = useState<string | undefined>();
  const [messages, setMessages] = useState<Message[]>([]);
  const [isPending, startTransition] = useTransition();
  const [showExamples, setShowExamples] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    if (conversationId && user?.id) {
      loadConversationHistory();
    }
  }, [conversationId, user?.id]);

  const loadConversationHistory = async () => {
    if (!conversationId || !user?.id) return;

    try {
      const history = await apiService.getConversationHistory(conversationId, user.id);
      setMessages(history.messages);
      setShowExamples(false);
    } catch (error) {
      console.error('Error loading conversation history:', error);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim() || isPending) return;

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

    startTransition(() => {
      (async () => {
        try {
          const response: ConversationResponse = await apiService.sendMessage(
            currentMessage,
            conversationId,
            user.id
          );
          
          setConversationId(response.conversationId);

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
        }
      })();
    });
  };

  const handleExampleClick = (example: string) => {
    setMessage(example);
    setShowExamples(false);
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-b from-background via-yellow-50/20 to-orange-50/30 dark:from-background dark:via-yellow-950/10 dark:to-orange-950/10">
      {/* Header */}
      <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            {/* Logo */}
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold">tideflow</h1>
            </div>

            {/* Right side buttons */}
            <div className="flex items-center gap-3">
              {user?.name && (
                <span className="text-sm text-muted-foreground">{user.name}</span>
              )}
              <Button variant="outline" size="sm" onClick={logout}>
                Sair
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Chat Area */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {messages.length === 0 && showExamples ? (
          // Initial state with title and examples
          <div className="flex-1 flex flex-col items-center justify-center px-4 py-12">
            <div className="w-full max-w-3xl space-y-8">
              <div className="text-center">
                <h2 className="text-5xl font-bold mb-4">Como posso ajudar?</h2>
              </div>

              {/* Example Queries */}
              <div className="space-y-3">
                <p className="text-sm text-muted-foreground text-center">Exemplos de perguntas:</p>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                  {EXAMPLE_QUERIES.map((example, index) => (
                    <button
                      key={index}
                      onClick={() => handleExampleClick(example)}
                      className="flex items-center justify-between p-3 text-left bg-card border rounded-lg hover:bg-accent hover:border-accent-foreground/20 transition-colors group"
                    >
                      <span className="text-sm">{example}</span>
                      <ChevronRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground transition-colors" />
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        ) : (
          // Chat messages
          <div className="flex-1 overflow-y-auto px-4 py-6">
            <div className="max-w-3xl mx-auto space-y-4">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={cn(
                    "flex w-full",
                    msg.role === 'USER' ? 'justify-end' : 'justify-start'
                  )}
                >
                  <div
                    className={cn(
                      "max-w-[80%] rounded-lg px-4 py-2",
                      msg.role === 'USER'
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-card border'
                    )}
                  >
                    <p className="text-sm whitespace-pre-wrap break-words">{msg.content}</p>
                  </div>
                </div>
              ))}
              {isPending && (
                <div className="flex justify-start">
                  <div className="bg-card border rounded-lg px-4 py-2">
                    <div className="flex gap-1">
                      <div className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                      <div className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                      <div className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
          </div>
        )}

        {/* Input Area - Fixed at bottom */}
        <div className="sticky bottom-0 border-t bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
          <div className="container mx-auto px-4 py-4">
            <form onSubmit={handleSubmit} className="w-full max-w-3xl mx-auto">
              <div className="flex items-center gap-2 p-4 bg-card border rounded-lg shadow-sm">
                {/* Input */}
                <Input
                  type="text"
                  placeholder="Pergunte qualquer coisa à IA"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  disabled={isPending}
                  className="flex-1 border-0 shadow-none focus-visible:ring-0 text-lg"
                />

                {/* Right side icons */}
                <div className="flex items-center gap-2">
                  <Button
                    type="submit"
                    size="icon"
                    disabled={!message.trim() || isPending}
                    className="h-8 w-8 bg-black dark:bg-white text-white dark:text-black hover:bg-black/90 dark:hover:bg-white/90"
                  >
                    <ArrowUp className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </main>
    </div>
  );
}
