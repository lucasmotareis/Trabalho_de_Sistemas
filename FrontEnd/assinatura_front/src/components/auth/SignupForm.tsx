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
import type { SignupRequest } from "@/types/auth";

const initialForm: SignupRequest = {
  nome: "",
  email: "",
  password: "",
};

export default function SignupForm() {
  const router = useRouter();
  const [form, setForm] = useState<SignupRequest>(initialForm);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleInputChange =
    (field: keyof SignupRequest) =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (!form.nome || !form.email || !form.password) {
      setError("Preencha nome, email e senha.");
      return;
    }

    setIsSubmitting(true);
    try {
      await authService.signup(form);
      router.push("/login");
    } catch (submitError) {
      const message =
        submitError instanceof Error
          ? submitError.message
          : "Nao foi possivel criar a conta.";
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthCardShell
      title="Criar conta"
      subtitle="Registre-se para assinar e gerenciar suas assinaturas digitais."
    >
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Stack spacing={2}>
          {error ? <Alert severity="error">{error}</Alert> : null}
          <TextField
            label="Nome completo"
            value={form.nome}
            onChange={handleInputChange("nome")}
            autoComplete="name"
            required
            fullWidth
          />
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
            autoComplete="new-password"
            required
            fullWidth
          />
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            {isSubmitting ? "Criando conta..." : "Criar conta"}
          </Button>
          <Typography variant="body2" color="text.secondary" textAlign="center">
            Ja possui conta?{" "}
            <Link component={NextLink} href="/login" underline="hover">
              Entrar
            </Link>
          </Typography>
        </Stack>
      </Box>
    </AuthCardShell>
  );
}
