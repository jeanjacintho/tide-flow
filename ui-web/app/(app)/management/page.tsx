'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { apiService, Department, User, CreateDepartmentRequest, InviteUserRequest } from '@/lib/api';
import { Building2, Users, Plus, Loader2, Trash2, Edit2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { toast } from 'sonner';
import { useRouter } from 'next/navigation';

export default function ManagementPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [isDepartmentDialogOpen, setIsDepartmentDialogOpen] = useState(false);
  const [isUserDialogOpen, setIsUserDialogOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [departmentFormData, setDepartmentFormData] = useState<CreateDepartmentRequest>({
    name: '',
    description: '',
  });
  const [userFormData, setUserFormData] = useState<InviteUserRequest>({
    name: '',
    email: '',
    departmentId: '',
    username: '',
    employeeId: '',
  });

  useEffect(() => {
    if (user?.companyRole === 'OWNER' && user?.companyId) {
      loadData();
    } else if (user && user.companyRole !== 'OWNER') {
      router.push('/dashboard');
    }
  }, [user, router]);

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

  const handleCreateDepartment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.companyId) return;

    setIsSubmitting(true);
    try {
      await apiService.createDepartment(user.companyId, departmentFormData);
      toast.success('Departamento criado com sucesso');
      setIsDepartmentDialogOpen(false);
      setDepartmentFormData({ name: '', description: '' });
      loadData();
    } catch (error) {
      toast.error('Erro ao criar departamento', {
        description: error instanceof Error ? error.message : 'Erro desconhecido',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleInviteUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.companyId) return;

    if (!userFormData.departmentId) {
      toast.error('Selecione um departamento');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await apiService.inviteUser(user.companyId, userFormData);
      toast.success('Funcionário convidado com sucesso', {
        description: `Usuário: ${response.username}, Senha temporária: ${response.temporaryPassword}`,
        duration: 10000,
      });
      setIsUserDialogOpen(false);
      setUserFormData({
        name: '',
        email: '',
        departmentId: '',
        username: '',
        employeeId: '',
      });
      loadData();
    } catch (error) {
      toast.error('Erro ao convidar funcionário', {
        description: error instanceof Error ? error.message : 'Erro desconhecido',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteDepartment = async (departmentId: string) => {
    if (!confirm('Tem certeza que deseja excluir este departamento?')) {
      return;
    }

    try {
      await apiService.deleteDepartment(departmentId);
      toast.success('Departamento excluído com sucesso');
      loadData();
    } catch (error) {
      toast.error('Erro ao excluir departamento', {
        description: error instanceof Error ? error.message : 'Erro desconhecido',
      });
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  if (user?.companyRole !== 'OWNER') {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <p className="text-lg font-semibold">Acesso negado</p>
          <p className="text-muted-foreground">Apenas o dono da empresa pode acessar esta página</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Gerenciamento</h1>
        <p className="text-muted-foreground">Gerencie departamentos e funcionários da sua empresa</p>
      </div>

      <Tabs defaultValue="departments" className="flex-1 flex flex-col overflow-hidden">
        <TabsList>
          <TabsTrigger value="departments">
            <Building2 className="w-4 h-4 mr-2" />
            Departamentos
          </TabsTrigger>
          <TabsTrigger value="users">
            <Users className="w-4 h-4 mr-2" />
            Funcionários
          </TabsTrigger>
        </TabsList>

        <TabsContent value="departments" className="flex-1 flex flex-col overflow-hidden mt-4">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-semibold">Departamentos</h2>
            <Dialog open={isDepartmentDialogOpen} onOpenChange={setIsDepartmentDialogOpen}>
              <DialogTrigger asChild>
                <Button>
                  <Plus className="w-4 h-4 mr-2" />
                  Novo Departamento
                </Button>
              </DialogTrigger>
              <DialogContent>
                <form onSubmit={handleCreateDepartment}>
                  <DialogHeader>
                    <DialogTitle>Criar Departamento</DialogTitle>
                    <DialogDescription>
                      Adicione um novo departamento à sua empresa
                    </DialogDescription>
                  </DialogHeader>
                  <div className="grid gap-4 py-4">
                    <div className="grid gap-2">
                      <Label htmlFor="name">Nome *</Label>
                      <Input
                        id="name"
                        value={departmentFormData.name}
                        onChange={(e) =>
                          setDepartmentFormData({ ...departmentFormData, name: e.target.value })
                        }
                        required
                      />
                    </div>
                    <div className="grid gap-2">
                      <Label htmlFor="description">Descrição</Label>
                      <Textarea
                        id="description"
                        value={departmentFormData.description}
                        onChange={(e) =>
                          setDepartmentFormData({ ...departmentFormData, description: e.target.value })
                        }
                        rows={3}
                      />
                    </div>
                  </div>
                  <DialogFooter>
                    <Button type="button" variant="outline" onClick={() => setIsDepartmentDialogOpen(false)}>
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

          <div className="flex-1 overflow-auto border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Descrição</TableHead>
                  <TableHead>Data de Criação</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {departments.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground">
                      Nenhum departamento cadastrado
                    </TableCell>
                  </TableRow>
                ) : (
                  departments.map((department) => (
                    <TableRow key={department.id}>
                      <TableCell className="font-medium">{department.name}</TableCell>
                      <TableCell>{department.description || '-'}</TableCell>
                      <TableCell>
                        {new Date(department.createdAt).toLocaleDateString('pt-BR')}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteDepartment(department.id)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </TabsContent>

        <TabsContent value="users" className="flex-1 flex flex-col overflow-hidden mt-4">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-semibold">Funcionários</h2>
            <Dialog open={isUserDialogOpen} onOpenChange={setIsUserDialogOpen}>
              <DialogTrigger asChild>
                <Button>
                  <Plus className="w-4 h-4 mr-2" />
                  Convidar Funcionário
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-md">
                <form onSubmit={handleInviteUser}>
                  <DialogHeader>
                    <DialogTitle>Convidar Funcionário</DialogTitle>
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
                  </div>
                  <DialogFooter>
                    <Button type="button" variant="outline" onClick={() => setIsUserDialogOpen(false)}>
                      Cancelar
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                      {isSubmitting && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                      Convidar
                    </Button>
                  </DialogFooter>
                </form>
              </DialogContent>
            </Dialog>
          </div>

          <div className="flex-1 overflow-auto border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Departamento</TableHead>
                  <TableHead>Username</TableHead>
                  <TableHead>Data de Criação</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground">
                      Nenhum funcionário cadastrado
                    </TableCell>
                  </TableRow>
                ) : (
                  users.map((userItem) => {
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
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
