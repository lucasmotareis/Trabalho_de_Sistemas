"use client";

import { useEffect, useState } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { keysService } from "@/services/keys.service";
import type { PublicKeyItem } from "@/types/keys";

export default function PublicKeysPage() {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [items, setItems] = useState<PublicKeyItem[]>([]);

  useEffect(() => {
    let active = true;

    const fetchPublicKeys = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await keysService.listPublicKeys();
        if (active) {
          setItems(response);
        }
      } catch (fetchError) {
        if (active) {
          const message =
            fetchError instanceof Error
              ? fetchError.message
              : "Nao foi possivel carregar as chaves publicas.";
          setError(message);
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    fetchPublicKeys();

    return () => {
      active = false;
    };
  }, []);

  return (
    <Box sx={{ maxWidth: 900, mx: "auto", p: { xs: 2, sm: 4 } }}>
      <Stack spacing={3}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Chaves publicas
        </Typography>

        {isLoading ? (
          <Stack direction="row" spacing={2} alignItems="center">
            <CircularProgress size={24} />
            <Typography variant="body2">Carregando chaves...</Typography>
          </Stack>
        ) : null}

        {error ? <Alert severity="error">{error}</Alert> : null}

        {!isLoading && !error && items.length === 0 ? (
          <Card variant="outlined" sx={{ p: 3 }}>
            <Typography variant="body2" color="text.secondary">
              Nenhuma chave publica disponivel.
            </Typography>
          </Card>
        ) : null}

        {!isLoading && !error && items.length > 0 ? (
          <Stack spacing={2}>
            {items.map((item) => (
              <Card key={`${item.userId}-${item.email}`} variant="outlined" sx={{ p: 3 }}>
                <Stack spacing={1.25}>
                  <Typography variant="body2">
                    <strong>User ID:</strong> {item.userId}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Email:</strong> {item.email}
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
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
                    {item.publicKey}
                  </Box>
                </Stack>
              </Card>
            ))}
          </Stack>
        ) : null}
      </Stack>
    </Box>
  );
}
