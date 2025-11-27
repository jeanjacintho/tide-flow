'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useRequireRole } from '@/hooks/useRequireRole';
import { apiService, Department, CreateDepartmentRequest } from '@/lib/api';
import { Building2, Plus, Loader2, Trash2 } from 'lucide-react';
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
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from 'sonner';

export default function AttendancePage() {
  const { hasAccess, isChecking } = useRequireRole({ 
    companyRole: 'OWNER',
    redirectTo: '/chat'
  });
  
  if (isChecking || !hasAccess) {
    return null;
  }
  
  const { user } = useAuth();
  
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [isDepartmentDialogOpen, setIsDepartmentDialogOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [departmentFormData, setDepartmentFormData] = useState<CreateDepartmentRequest>({
    name: '',
    description: '',
  });

  useEffect(() => {
    if (user?.companyRole === 'OWNER' && user?.companyId) {
      loadDepartments();
    }
  }, [user]);

  const loadDepartments = async () => {
    if (!user?.companyId) return;
    
    try {
      setLoading(true);
      const departmentsData = await apiService.getCompanyDepartments(user.companyId);
      setDepartments(departmentsData);
    } catch (error) {
      toast.error('Erro ao carregar departamentos', {
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
      loadDepartments();
    } catch (error) {
      toast.error('Erro ao criar departamento', {
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
      loadDepartments();
    } catch (error) {
      toast.error('Erro ao excluir departamento', {
        description: error instanceof Error ? error.message : 'Erro desconhecido',
      });
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Departamentos</h1>
          <p className="text-muted-foreground mt-1">
            Gerencie os departamentos da sua empresa
          </p>
        </div>
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

      {loading ? (
        <Card>
          <CardHeader>
            <CardTitle>Departamentos</CardTitle>
            <CardDescription>
              Lista de todos os departamentos da empresa
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-center h-64">
              <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
            </div>
          </CardContent>
        </Card>
      ) : departments.length === 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>Departamentos</CardTitle>
            <CardDescription>
              Lista de todos os departamentos da empresa
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col items-center justify-center h-64 text-center">
              <Building2 className="w-12 h-12 text-muted-foreground mb-4" />
              <p className="text-lg font-semibold">Nenhum departamento cadastrado</p>
              <p className="text-muted-foreground">Comece criando um novo departamento</p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>Departamentos</CardTitle>
            <CardDescription>
              Lista de todos os departamentos da empresa
            </CardDescription>
          </CardHeader>
          <CardContent>
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
                {departments.map((department) => (
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
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
