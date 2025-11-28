'use client';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { User as UserType } from '@/lib/api';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

interface AccountInfoCardProps {
  user: UserType | null;
  loading?: boolean;
}

export function AccountInfoCard({ user, loading = false }: AccountInfoCardProps) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-32" />
          <Skeleton className="h-4 w-48 mt-2" />
        </CardHeader>
        <CardContent className="space-y-4">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-16 w-full" />
        </CardContent>
      </Card>
    );
  }

  if (!user) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Informações da Conta</CardTitle>
          <CardDescription>Nenhuma informação disponível</CardDescription>
        </CardHeader>
      </Card>
    );
  }

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  const getRoleBadgeVariant = (role?: string) => {
    if (!role) return 'secondary';
    if (role.includes('ADMIN') || role.includes('OWNER')) return 'default';
    if (role.includes('MANAGER')) return 'secondary';
    return 'outline';
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Informações da Conta</CardTitle>
        <CardDescription>Dados pessoais e permissões do usuário</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {}
        <div className="flex items-start gap-4">
          <Avatar className="h-16 w-16">
            <AvatarImage src={user.avatarUrl || undefined} />
            <AvatarFallback className="text-lg">
              {getInitials(user.name)}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1 space-y-2">
            <div>
              <h3 className="text-lg font-semibold">{user.name}</h3>
              <div className="flex items-center gap-2 mt-1">
                {user.systemRole && (
                  <Badge variant={getRoleBadgeVariant(user.systemRole)}>
                    {user.systemRole}
                  </Badge>
                )}
                {user.companyRole && (
                  <Badge variant={getRoleBadgeVariant(user.companyRole)}>
                    {user.companyRole}
                  </Badge>
                )}
              </div>
            </div>
          </div>
        </div>

        {}
        <div className="space-y-3">
          <div className="text-sm">
            <p className="text-muted-foreground">Email</p>
            <p className="font-medium">{user.email}</p>
          </div>

          {user.trustedEmail && (
            <div className="text-sm">
              <p className="text-muted-foreground">Email de Confiança</p>
              <p className="font-medium">{user.trustedEmail}</p>
            </div>
          )}

          {user.phone && (
            <div className="text-sm">
              <p className="text-muted-foreground">Telefone</p>
              <p className="font-medium">{user.phone}</p>
            </div>
          )}

          {(user.city || user.state) && (
            <div className="text-sm">
              <p className="text-muted-foreground">Localização</p>
              <p className="font-medium">
                {[user.city, user.state].filter(Boolean).join(', ') || 'Não informado'}
              </p>
            </div>
          )}

          <div className="text-sm">
            <p className="text-muted-foreground">Membro desde</p>
            <p className="font-medium">
              {user.createdAt
                ? format(new Date(user.createdAt), "dd 'de' MMMM 'de' yyyy", { locale: ptBR })
                : 'Não disponível'}
            </p>
          </div>
        </div>

        {}
        {user.privacyConsentStatus && (
          <div className="pt-4 border-t">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium">Status de Privacidade</p>
                <p className="text-xs text-muted-foreground mt-1">
                  {user.dataSharingEnabled
                    ? 'Dados agregados podem ser compartilhados'
                    : 'Dados agregados não compartilhados'}
                </p>
              </div>
              <Badge
                variant={
                  user.privacyConsentStatus === 'ACCEPTED'
                    ? 'default'
                    : user.privacyConsentStatus === 'DENIED'
                    ? 'destructive'
                    : 'secondary'
                }
              >
                {user.privacyConsentStatus}
              </Badge>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
