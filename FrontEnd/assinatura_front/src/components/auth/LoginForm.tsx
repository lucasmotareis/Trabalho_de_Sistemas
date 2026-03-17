"use client";

import { useState } from "react";
import NextLink from "next/link";
import { useRouter } from "next/navigation";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import AuthCardShell from "./AuthCardShell";
import { authService } from "@/services/auth.service";
import { setStoredSession } from "@/lib/auth";
import type { LoginRequest } from "@/types/auth";

const initialForm: LoginRequest = {
  email: "",
  password: "",
};

export default function LoginForm() {
  const router = useRouter();
  const [form, setForm] = useState<LoginRequest>(initialForm);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleInputChange =
    (field: keyof LoginRequest) =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (!form.email || !form.password) {
      setError("Informe email e senha para continuar.");
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await authService.login(form);
      setStoredSession({
        user: {
          id: String(response.id),
          name: response.nome,
          email: response.email,
        },
      });
      router.push("/sign");
    } catch (submitError) {
      const message =
        submitError instanceof Error
          ? submitError.message
          : "Nao foi possivel fazer login.";
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthCardShell
      title="Entrar"
      subtitle="Acesse sua conta para assinar documentos digitais."
    >
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Stack spacing={2}>
          {error ? <Alert severity="error">{error}</Alert> : null}
          <TextField
            label="Email"
            type="email"
            value={form.email}
            onChange={handleInputChange("email")}
            autoComplete="email"
            required
            fullWidth
          />
          <TextField
            label="Senha"
            type="password"
            value={form.password}
            onChange={handleInputChange("password")}
            autoComplete="current-password"
            required
            fullWidth
          />
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            {isSubmitting ? "Entrando..." : "Entrar"}
          </Button>
          <Typography variant="body2" color="text.secondary" textAlign="center">
            Nao tem conta?{" "}
            <Link component={NextLink} href="/signup" underline="hover">
              Criar conta
            </Link>
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center">
            Verificação pública: {" "}
            <Link component={NextLink} href="/verify" underline="hover">
              Verificar assinatura
            </Link>
          </Typography>
        </Stack>
      </Box>
    </AuthCardShell>
  );
}
