'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function Login() {
    const router = useRouter();

    useEffect(() => {
        // Redireciona para a página de login de usuário por padrão
        router.replace('/login/user');
    }, [router]);

    return null;
}