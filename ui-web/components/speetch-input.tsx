'use client';

import { Mic, MicOff } from "lucide-react";
import { Button } from "./ui/button";
import { useEffect, useState, useRef } from "react";
import { cn } from "@/lib/utils";

interface SpeechInputProps {
  onTranscriptChange?: (transcript: string) => void;
  disabled?: boolean;
  conversationId?: string;
  userId?: string;
}

export default function SpeechInput({ 
  onTranscriptChange, 
  disabled = false,
  conversationId,
  userId
}: SpeechInputProps) {
  const [isRecording, setIsRecording] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);

  // Limpa recursos quando o componente desmonta
  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
      }
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
    };
  }, []);

  const handleToggleRecording = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (isProcessing) {
      return;
    }

    if (isRecording) {
      // Parar gravação
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
        streamRef.current = null;
      }
      setIsRecording(false);
    } else {
      // Iniciar gravação
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        streamRef.current = stream;

        const mediaRecorder = new MediaRecorder(stream, {
          mimeType: 'audio/webm;codecs=opus'
        });
        mediaRecorderRef.current = mediaRecorder;
        audioChunksRef.current = [];

        mediaRecorder.ondataavailable = (event) => {
          if (event.data.size > 0) {
            audioChunksRef.current.push(event.data);
          }
        };

        mediaRecorder.onstop = async () => {
          setIsProcessing(true);
          
          try {
            // Cria um blob do áudio gravado
            const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
            
            // Envia para o backend para transcrição
            const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
            const formData = new FormData();
            formData.append('audio', audioBlob, 'recording.webm');
            if (conversationId) {
              formData.append('conversationId', conversationId);
            }

            const response = await fetch(`${AI_SERVICE_URL}/api/conversations/transcribe`, {
              method: 'POST',
              headers: {
                'X-User-Id': userId || '',
              },
              body: formData,
            });

            if (!response.ok) {
              throw new Error(`Erro ao transcrever áudio: ${response.status}`);
            }

            const data = await response.json();
            
            // Atualiza o transcript
            if (data.transcript && onTranscriptChange) {
              onTranscriptChange(data.transcript);
            }

            // Se houver resposta da conversa, também atualiza
            if (data.conversationResponse) {
              // A resposta da IA já foi processada no backend
              console.log('Conversa processada:', data.conversationResponse);
            }
          } catch (error) {
            console.error('Erro ao transcrever áudio:', error);
            alert(error instanceof Error ? error.message : 'Erro ao transcrever áudio');
          } finally {
            setIsProcessing(false);
            audioChunksRef.current = [];
          }
        };

        mediaRecorder.start();
        setIsRecording(true);
      } catch (error) {
        console.error('Erro ao iniciar gravação:', error);
        alert('Erro ao iniciar gravação. Verifique as permissões do microfone.');
      }
    }
  };

  // Verifica suporte do navegador
  if (typeof window === 'undefined' || !navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    return null;
  }

  return (
    <Button
      type="button"
      size="icon"
      variant={isRecording ? "destructive" : "ghost"}
      onClick={handleToggleRecording}
      disabled={disabled || isProcessing}
      className={cn(
        "h-9 w-9 border border-primary text-primary rounded-lg shadow-sm",
        isRecording && "animate-pulse"
      )}
      title={isRecording ? "Parar gravação" : "Iniciar gravação"}
    >
      {isRecording ? (
        <MicOff className="w-5 h-5" />
      ) : (
        <Mic className="w-5 h-5" />
      )}
    </Button>
  );
}
