"use client";

import { useEffect, useState } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { keysService } from "@/services/keys.service";
import type { MyKeysResponse } from "@/types/keys";

function hiddenPrivateKey(value: string) {
  return "•".repeat(Math.max(12, Math.min(value.length, 96)));
}

export default function MyKeysCard() {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [keys, setKeys] = useState<MyKeysResponse | null>(null);
  const [showPrivateKey, setShowPrivateKey] = useState(false);

  useEffect(() => {
    let active = true;

    const fetchMyKeys = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await keysService.getMyKeys();
        if (active) {
          setKeys(response);
        }
      } catch (fetchError) {
        if (active) {
          const message =
            fetchError instanceof Error
              ? fetchError.message
              : "Nao foi possivel carregar suas chaves.";
          setError(message);
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    fetchMyKeys();

    return () => {
      active = false;
    };
  }, []);

  return (
    <Stack spacing={3}>
      <Typography variant="h4" sx={{ fontWeight: 700 }}>
        Minhas chaves
      </Typography>

      {isLoading ? (
        <Stack direction="row" spacing={2} alignItems="center">
          <CircularProgress size={24} />
          <Typography variant="body2">Carregando suas chaves...</Typography>
        </Stack>
      ) : null}

      {error ? <Alert severity="error">{error}</Alert> : null}

      {!isLoading && !error && keys ? (
        <Card variant="outlined" sx={{ p: 3 }}>
          <Stack spacing={1.5}>
            <Typography variant="body2">
              <strong>User ID:</strong> {keys.userId}
            </Typography>
            <Typography variant="body2">
              <strong>Email:</strong> {keys.email}
            </Typography>

            <Typography variant="body2" sx={{ mt: 1, fontWeight: 600 }}>
              Chave publica
            </Typography>
            <Box
              component="pre"
              sx={{
                m: 0,
                p: 2,
                borderRadius: 2,
                backgroundColor: "action.hover",
                whiteSpace: "pre-wrap",
                wordBreak: "break-all",
                fontFamily: 'Consolas, "Courier New", monospace',
                fontSize: 13,
              }}
            >
              {keys.publicKey}
            </Box>

            <Stack
              direction={{ xs: "column", sm: "row" }}
              spacing={1}
              alignItems={{ xs: "flex-start", sm: "center" }}
              justifyContent="space-between"
            >
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                Chave privada
              </Typography>
              <Button
                size="small"
                color="inherit"
                onClick={() => setShowPrivateKey((prev) => !prev)}
              >
                {showPrivateKey ? "Ocultar chave privada" : "Mostrar chave privada"}
              </Button>
            </Stack>
            <Box
              component="pre"
              sx={{
                m: 0,
                p: 2,
                borderRadius: 2,
                backgroundColor: "action.hover",
                whiteSpace: "pre-wrap",
                wordBreak: "break-all",
                fontFamily: 'Consolas, "Courier New", monospace',
                fontSize: 13,
              }}
            >
              {showPrivateKey ? keys.privateKey : hiddenPrivateKey(keys.privateKey)}
            </Box>
          </Stack>
        </Card>
      ) : null}
    </Stack>
  );
}
