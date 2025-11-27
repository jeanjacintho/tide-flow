'use client';

import { useRequireRole } from '@/hooks/useRequireRole';
import { useAuth } from '@/contexts/AuthContext';
import { useDashboardData } from '@/hooks/use-dashboard';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Users, UserPlus, CheckCircle2, Search, MoreVertical, ArrowUpDown, Briefcase, Calendar } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const MOCK_COMPANY_ID = '00000000-0000-0000-0000-000000000000';

// Mock data para os gráficos
const performanceData = [
  { month: 'Jan', value: 78 },
  { month: 'Feb', value: 80 },
  { month: 'Mar', value: 82 },
  { month: 'Apr', value: 85 },
  { month: 'May', value: 88 },
  { month: 'Jun', value: 95 },
  { month: 'Jul', value: 90 },
  { month: 'Aug', value: 87 },
  { month: 'Sep', value: 89 },
  { month: 'Oct', value: 91 },
  { month: 'Nov', value: 88 },
  { month: 'Dec', value: 89 },
];

const employeeTypeData = [
  { name: 'Full-Time', value: 92, color: 'hsl(var(--primary))' },
  { name: 'Part-Time', value: 25, color: 'hsl(var(--chart-2))' },
];

const employeeListData = [
  { id: 1, name: 'John Doe', department: 'Marketing', position: 'Digital Marketer', avatar: null },
  { id: 2, name: 'Maria Tan', department: 'Finance', position: 'Finance Manager', avatar: null },
  { id: 3, name: 'Sarah Johnson', department: 'HR', position: 'HR Specialist', avatar: null },
  { id: 4, name: 'Michael Chen', department: 'IT', position: 'Software Engineer', avatar: null },
];

const activityData = [
  { type: 'company-event', title: 'Annual Town Hall', date: 'April 10' },
  { type: 'holiday', title: 'Public Holiday', date: 'April 10' },
  { type: 'holiday', title: 'National Good Friday', date: 'April 7' },
];

export default function DashboardPage() {
  const { hasAccess, isChecking } = useRequireRole({ 
    companyRole: ['HR_MANAGER', 'ADMIN', 'OWNER'],
    redirectTo: '/chat'
  });
  
  const { user } = useAuth();
  
  if (isChecking || !hasAccess) {
    return null;
  }
  
  const { data: dashboardData, loading: dashboardLoading } = useDashboardData(MOCK_COMPANY_ID, new Date());

  const totalEmployees = dashboardData?.totalActiveUsers ?? 134;
  const newHires = 5;
  const absenteeRate = 3.2;
  const performance = 89;

  return (
    <div className="flex-1 overflow-y-auto p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground mt-1">
          Overview of your company's HR metrics and insights
        </p>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="hr" className="w-full">
        <TabsList className="mb-6">
          <TabsTrigger value="hr">HR Dashboard</TabsTrigger>
          <TabsTrigger value="recruitment">Recruitment Dashboard</TabsTrigger>
        </TabsList>

        <TabsContent value="hr" className="space-y-6">
          {/* Key Metric Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Employees</CardTitle>
                <Users className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                {dashboardLoading ? (
                  <Skeleton className="h-8 w-20" />
                ) : (
                  <>
                    <div className="text-2xl font-bold">{totalEmployees}</div>
                    <p className="text-xs text-green-600 mt-1">+2 from last month</p>
                  </>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">New Hires This Month</CardTitle>
                <UserPlus className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{newHires}</div>
                <p className="text-xs text-green-600 mt-1">+2 from last month</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Absentee Rate</CardTitle>
                <CheckCircle2 className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{absenteeRate}%</div>
                <p className="text-xs text-red-600 mt-1">-0.2% from last month</p>
              </CardContent>
            </Card>
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Employee Performance Chart */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle>Employee Performance</CardTitle>
                  <Select defaultValue="this-year">
                    <SelectTrigger className="w-[140px] h-8">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="this-year">This year</SelectItem>
                      <SelectItem value="last-year">Last year</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex items-baseline gap-2 mt-2">
                  <div className="text-3xl font-bold">{performance}%</div>
                  <p className="text-sm text-green-600">+2.4% from last year</p>
                </div>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={performanceData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="month" stroke="hsl(var(--muted-foreground))" />
                    <YAxis stroke="hsl(var(--muted-foreground))" />
                    <Tooltip 
                      contentStyle={{ 
                        backgroundColor: 'hsl(var(--background))',
                        border: '1px solid hsl(var(--border))',
                        borderRadius: '8px'
                      }}
                    />
                    <Bar 
                      dataKey="value" 
                      fill="hsl(var(--primary))"
                      radius={[8, 8, 0, 0]}
                    >
                      {performanceData.map((entry, index) => (
                        <Cell 
                          key={`cell-${index}`} 
                          fill={entry.month === 'Jun' ? 'hsl(var(--primary))' : 'hsl(var(--muted))'} 
                        />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
                <div className="flex items-center gap-2 mt-4">
                  <div className="h-4 w-4 bg-muted rounded"></div>
                  <span className="text-sm text-muted-foreground">Avg 82%</span>
                </div>
              </CardContent>
            </Card>

            {/* Employee Type Chart */}
            <Card>
              <CardHeader>
                <CardTitle>Employee Type</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center justify-center">
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={employeeTypeData}
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={100}
                        paddingAngle={5}
                        dataKey="value"
                      >
                        {employeeTypeData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="flex flex-col gap-2 mt-4">
                  {employeeTypeData.map((item, index) => (
                    <div key={index} className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div 
                          className="h-3 w-3 rounded-full" 
                          style={{ backgroundColor: item.color }}
                        ></div>
                        <span className="text-sm">{item.value} {item.name}</span>
                      </div>
                      <span className="text-sm text-muted-foreground">
                        {index === 0 ? '68' : '15'}
                      </span>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Bottom Row: Employee List and Activity */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Employee List */}
            <Card>
              <CardHeader>
                <CardTitle>Employee List</CardTitle>
                <div className="flex items-center gap-2 mt-4">
                  <Select defaultValue="all">
                    <SelectTrigger className="w-[180px]">
                      <SelectValue placeholder="Select department" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Departments</SelectItem>
                      <SelectItem value="marketing">Marketing</SelectItem>
                      <SelectItem value="finance">Finance</SelectItem>
                      <SelectItem value="hr">HR</SelectItem>
                      <SelectItem value="it">IT</SelectItem>
                    </SelectContent>
                  </Select>
                  <Select defaultValue="7d">
                    <SelectTrigger className="w-[140px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="7d">Last 7 days</SelectItem>
                      <SelectItem value="30d">Last 30 days</SelectItem>
                      <SelectItem value="90d">Last 90 days</SelectItem>
                    </SelectContent>
                  </Select>
                  <div className="relative flex-1">
                    <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input placeholder="Search employee" className="pl-8" />
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Contact / Company</TableHead>
                      <TableHead>
                        <div className="flex items-center gap-1">
                          Department
                          <ArrowUpDown className="h-3 w-3" />
                        </div>
                      </TableHead>
                      <TableHead>Position</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {employeeListData.map((employee) => (
                      <TableRow key={employee.id}>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <Avatar className="h-8 w-8">
                              <AvatarImage src={employee.avatar || undefined} />
                              <AvatarFallback>
                                {employee.name.split(' ').map(n => n[0]).join('')}
                              </AvatarFallback>
                            </Avatar>
                            <span className="font-medium">{employee.name}</span>
                          </div>
                        </TableCell>
                        <TableCell>{employee.department}</TableCell>
                        <TableCell>{employee.position}</TableCell>
                        <TableCell>
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" className="h-8 w-8">
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem>View</DropdownMenuItem>
                              <DropdownMenuItem>Edit</DropdownMenuItem>
                              <DropdownMenuItem>Delete</DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>

            {/* Activity Feed */}
            <Card>
              <CardHeader>
                <CardTitle>Activity</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {activityData.map((activity, index) => (
                    <div key={index} className="flex items-start gap-3">
                      <div className="mt-1">
                        {activity.type === 'company-event' ? (
                          <Briefcase className="h-4 w-4 text-muted-foreground" />
                        ) : (
                          <Calendar className="h-4 w-4 text-muted-foreground" />
                        )}
                      </div>
                      <div className="flex-1">
                        <p className="text-sm font-medium">{activity.title}</p>
                        <p className="text-xs text-muted-foreground">{activity.date}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="recruitment">
          <div className="text-center py-12">
            <p className="text-muted-foreground">Recruitment Dashboard coming soon...</p>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
