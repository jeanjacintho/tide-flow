'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useRequireRole } from '@/hooks/useRequireRole';
import { apiService, Department, User, CreateCompanyUserRequest, UsageInfo } from '@/lib/api';
import { Users, Plus, Loader2, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from 'sonner';
import { useRouter } from 'next/navigation';

export default function EmployeesPage() {
  const { hasAccess, isChecking } = useRequireRole({ 
    companyRole: 'OWNER',
    redirectTo: '/chat'
  });
  
  if (isChecking || !hasAccess) {
    return null;
  }
  
  const { user } = useAuth();
  const router = useRouter();
  
  const [departments, setDepartments] = useState<Department[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [isUserDialogOpen, setIsUserDialogOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [userFormData, setUserFormData] = useState<CreateCompanyUserRequest>({
    name: '',
    email: '',
    password: '',
    departmentId: '',
    username: '',
    employeeId: '',
    phone: '',
    city: '',
    state: '',
  });

  useEffect(() => {
    if (user?.companyRole === 'OWNER' && user?.companyId) {
      loadData();
    }
  }, [user]);

  const loadData = async () => {
    if (!user?.companyId) return;
    
    try {
      setLoading(true);
      const [departmentsData, usersData] = await Promise.all([
        apiService.getCompanyDepartments(user.companyId),
        apiService.getCompanyUsers(user.companyId),
      ]);
      setDepartments(departmentsData);
      setUsers(usersData);
    } catch (error) {
      toast.error('Erro ao carregar dados', {
        description: error instanceof Error ? error.message : 'Erro desconhecido',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.companyId) return;

    if (!userFormData.departmentId) {
      toast.error('Selecione um departamento');
      return;
    }

    if (!userFormData.password || userFormData.password.length < 8) {
      toast.error('A senha deve ter pelo menos 8 caracteres');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiService.createCompanyUser(user.companyId, userFormData);
      toast.success('Funcionário criado com sucesso');
      setIsUserDialogOpen(false);
      setUserFormData({
        name: '',
        email: '',
        password: '',
        departmentId: '',
        username: '',
        employeeId: '',
        phone: '',
        city: '',
        state: '',
      });
      loadData();
    } catch (error) {
      toast.error('Erro ao criar funcionário', {
        description: error instanceof Error ? error.message : 'Erro desconhecido',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteUser = async (userId: string) => {
    if (!confirm('Tem certeza que deseja excluir este funcionário?')) {
      return;
    }

    try {
      await apiService.deleteUser(userId);
      toast.success('Funcionário excluído com sucesso');
      loadData();
    } catch (error) {
      toast.error('Erro ao excluir funcionário', {
        description: error instanceof Error ? error.message : 'Erro desconhecido',
      });
    }
  };

  const handleCheckLimitBeforeCreate = async (e: React.MouseEvent) => {
    e.preventDefault();
    
    if (!user?.companyId) return;

    try {
      const usageInfo: UsageInfo = await apiService.getUsageInfo(user.companyId);
      
      if (usageInfo.atLimit || usageInfo.remainingSlots === 0) {
        toast.error('Limite de funcionários atingido', {
          description: `Você possui ${usageInfo.activeUsers} funcionários de ${usageInfo.maxUsers} permitidos no plano atual. Faça upgrade para o plano Enterprise para adicionar mais funcionários.`,
          duration: 6000,
          action: {
            label: 'Ver Assinaturas',
            onClick: () => router.push('/subscription'),
          },
        });
        return;
      }
      
      setIsUserDialogOpen(true);
    } catch (error) {
      console.error('Erro ao verificar limite:', error);
      setIsUserDialogOpen(true);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Diretório de Funcionários</h1>
          <p className="text-muted-foreground mt-1">
            Gerencie os funcionários da sua empresa
          </p>
        </div>
        <Dialog open={isUserDialogOpen} onOpenChange={setIsUserDialogOpen}>
          <Button onClick={handleCheckLimitBeforeCreate}>
            <Plus className="w-4 h-4 mr-2" />
            Criar Funcionário
          </Button>
          <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
            <form onSubmit={handleCreateUser}>
              <DialogHeader>
                <DialogTitle>Criar Funcionário</DialogTitle>
                <DialogDescription>
                  Adicione um novo funcionário à sua empresa
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid gap-2">
                  <Label htmlFor="user-name">Nome *</Label>
                  <Input
                    id="user-name"
                    value={userFormData.name}
                    onChange={(e) =>
                      setUserFormData({ ...userFormData, name: e.target.value })
                    }
                    required
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="user-email">Email *</Label>
                  <Input
                    id="user-email"
                    type="email"
                    value={userFormData.email}
                    onChange={(e) =>
                      setUserFormData({ ...userFormData, email: e.target.value })
                    }
                    required
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="user-password">Senha *</Label>
                  <Input
                    id="user-password"
                    type="password"
                    value={userFormData.password}
                    onChange={(e) =>
                      setUserFormData({ ...userFormData, password: e.target.value })
                    }
                    required
                    minLength={8}
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="user-department">Departamento *</Label>
                  <Select
                    value={userFormData.departmentId}
                    onValueChange={(value) =>
                      setUserFormData({ ...userFormData, departmentId: value })
                    }
                    required
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Selecione um departamento" />
                    </SelectTrigger>
                    <SelectContent>
                      {departments.map((dept) => (
                        <SelectItem key={dept.id} value={dept.id}>
                          {dept.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="user-username">Username (opcional)</Label>
                  <Input
                    id="user-username"
                    value={userFormData.username}
                    onChange={(e) =>
                      setUserFormData({ ...userFormData, username: e.target.value })
                    }
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="user-employee-id">ID do Funcionário (opcional)</Label>
                  <Input
                    id="user-employee-id"
                    value={userFormData.employeeId}
                    onChange={(e) =>
                      setUserFormData({ ...userFormData, employeeId: e.target.value })
                    }
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="user-phone">Telefone (opcional)</Label>
                  <Input
                    id="user-phone"
                    type="tel"
                    value={userFormData.phone}
                    onChange={(e) =>
                      setUserFormData({ ...userFormData, phone: e.target.value })
                    }
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className="grid gap-2">
                    <Label htmlFor="user-city">Cidade (opcional)</Label>
                    <Input
                      id="user-city"
                      value={userFormData.city}
                      onChange={(e) =>
                        setUserFormData({ ...userFormData, city: e.target.value })
                      }
                    />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="user-state">Estado (opcional)</Label>
                    <Input
                      id="user-state"
                      value={userFormData.state}
                      onChange={(e) =>
                        setUserFormData({ ...userFormData, state: e.target.value })
                      }
                      maxLength={2}
                    />
                  </div>
                </div>
              </div>
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setIsUserDialogOpen(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                  Criar
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {loading ? (
        <Card>
          <CardHeader>
            <CardTitle>Diretório de Funcionários</CardTitle>
            <CardDescription>
              Lista de todos os funcionários da empresa
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-center h-64">
              <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
            </div>
          </CardContent>
        </Card>
      ) : users.length === 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>Diretório de Funcionários</CardTitle>
            <CardDescription>
              Lista de todos os funcionários da empresa
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col items-center justify-center h-64 text-center">
              <Users className="w-12 h-12 text-muted-foreground mb-4" />
              <p className="text-lg font-semibold">Nenhum funcionário cadastrado</p>
              <p className="text-muted-foreground">Comece criando um novo funcionário</p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>Diretório de Funcionários</CardTitle>
            <CardDescription>
              Lista de todos os funcionários da empresa
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Departamento</TableHead>
                  <TableHead>Username</TableHead>
                  <TableHead>Data de Criação</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.map((userItem) => {
                  const department = departments.find((d) => d.id === userItem.departmentId);
                  return (
                    <TableRow key={userItem.id}>
                      <TableCell className="font-medium">{userItem.name}</TableCell>
                      <TableCell>{userItem.email}</TableCell>
                      <TableCell>{department?.name || '-'}</TableCell>
                      <TableCell>{userItem.username || '-'}</TableCell>
                      <TableCell>
                        {new Date(userItem.createdAt).toLocaleDateString('pt-BR')}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteUser(userItem.id)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
