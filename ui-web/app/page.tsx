import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function Home() {
  return (
    <div className="flex flex-col min-h-screen">
      <header className="border-b">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold bg-gradient-to-r from-black via-purple-500 to-blue-500 bg-clip-text text-transparent">
            Tideflow
          </h1>
          <nav className="flex gap-4">
            <Link href="/login">
              <Button variant="ghost">Entrar</Button>
            </Link>
            <Link href="/register">
              <Button>Registrar</Button>
            </Link>
          </nav>
        </div>
      </header>
      
      <main className="flex-1 flex items-center justify-center">
        <div className="container mx-auto px-4 text-center">
          <h2 className="text-6xl font-bold mb-6 bg-gradient-to-r from-black via-purple-500 to-blue-500 bg-clip-text text-transparent">
            Cuide da sua saúde e bem-estar
          </h2>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            Tideflow é um aplicativo completo para você gerenciar sua saúde e bem-estar de forma simples e eficiente.
          </p>
          <div className="flex gap-4 justify-center">
            <Link href="/register">
              <Button size="lg">Começar agora</Button>
            </Link>
            <Link href="/login">
              <Button size="lg" variant="outline">Já tenho conta</Button>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
