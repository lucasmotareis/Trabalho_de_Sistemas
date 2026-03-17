"use client";

import { useEffect, useState } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import NextLink from "next/link";
import { signService } from "@/services/sign.service";
import type { SignTextResponse } from "@/types/signature";
import { formatTimestamp } from "@/lib/formatters";
import {
  appendSignatureHistory,
  clearSignatureHistory,
  getSessionUserHistoryKey,
  getSignatureHistory,
} from "@/lib/signatureHistory";

function getSignaturePreview(signatureBase64: string) {
  return signatureBase64.length > 80
    ? `${signatureBase64.slice(0, 80)}...`
    : signatureBase64;
}

export default function SignTextCard() {
  const [text, setText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SignTextResponse | null>(null);
  const [history, setHistory] = useState<SignTextResponse[]>([]);
  const [historyUserKey, setHistoryUserKey] = useState<string | null>(null);

  useEffect(() => {
    const userKey = getSessionUserHistoryKey();
    if (!userKey) {
      setHistory([]);
      setResult(null);
      setHistoryUserKey(null);
      return;
    }

    const hydrated = getSignatureHistory(userKey);
    setHistoryUserKey(userKey);
    setHistory(hydrated);
    setResult(hydrated[0] ?? null);
  }, []);

  const handleSign = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (!text.trim()) {
      setError("Digite um texto para assinar.");
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await signService.signText({ text: text.trim() });
      setResult(response);
      setText("");
      if (historyUserKey) {
        appendSignatureHistory(historyUserKey, response);
        setHistory(getSignatureHistory(historyUserKey));
      }
    } catch (submitError) {
      const message =
        submitError instanceof Error
          ? submitError.message
          : "Nao foi possivel assinar o texto.";
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Typography variant="h4" sx={{ fontWeight: 700 }}>
        Assinar texto
      </Typography>
      <Card variant="outlined" sx={{ p: 3 }}>
        <Box component="form" onSubmit={handleSign}>
          <Stack spacing={2}>
            {error ? <Alert severity="error">{error}</Alert> : null}
            <TextField
              label="Texto"
              multiline
              minRows={8}
              value={text}
              onChange={(event) => setText(event.target.value)}
              placeholder="Digite ou cole o texto que sera assinado"
              fullWidth
              required
            />
            <Button type="submit" variant="contained" disabled={isSubmitting}>
              {isSubmitting ? "Assinando..." : "Assinar"}
            </Button>
          </Stack>
        </Box>
      </Card>

      {result ? (
        <Card variant="outlined" sx={{ p: 3 }}>
          <Stack spacing={1.5}>
            <Typography variant="h6">Resultado da assinatura</Typography>
            <Typography variant="body2">
              <strong>ID interno:</strong> {result.id}
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 700 }}>
              <strong>ID publico:</strong> {result.publicId}
            </Typography>
            <Typography variant="body2">
              <strong>Algoritmo:</strong> {result.signatureAlgorithm}
            </Typography>
            <Typography variant="body2">
              <strong>Data:</strong>{" "}
              {result.createdAt ? formatTimestamp(result.createdAt) : "N/A"}
            </Typography>
            <Link component={NextLink} href={`/verify/${result.publicId}`} underline="hover">
              Verificar assinatura publica
            </Link>
            <Typography variant="body2" sx={{ mt: 1, fontWeight: 600 }}>
              Assinatura
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
              {result.signatureBase64}
            </Box>
          </Stack>
        </Card>
      ) : null}

      <Card variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={1}
            alignItems={{ xs: "flex-start", sm: "center" }}
            justifyContent="space-between"
          >
            <Typography variant="h6">Historico de assinaturas</Typography>
            {historyUserKey ? (
              <Button
                size="small"
                color="inherit"
                onClick={() => {
                  clearSignatureHistory(historyUserKey);
                  setHistory([]);
                  setResult(null);
                }}
              >
                Limpar historico
              </Button>
            ) : null}
          </Stack>

          {history.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              Nenhuma assinatura salva neste navegador para o usuario atual.
            </Typography>
          ) : (
            <Stack spacing={1.5}>
              {history.map((item) => (
                <Card key={`${item.id}-${item.publicId}`} variant="outlined" sx={{ p: 2 }}>
                  <Stack spacing={0.75}>
                    <Typography variant="body2">
                      <strong>ID interno:</strong> {item.id}
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700 }}>
                      <strong>ID publico:</strong> {item.publicId}
                    </Typography>
                    <Typography variant="body2">
                      <strong>Algoritmo:</strong> {item.signatureAlgorithm}
                    </Typography>
                    <Typography variant="body2">
                      <strong>Data:</strong>{" "}
                      {item.createdAt ? formatTimestamp(item.createdAt) : "N/A"}
                    </Typography>
                    <Typography variant="body2">
                      <strong>Assinatura (preview):</strong>{" "}
                      {getSignaturePreview(item.signatureBase64)}
                    </Typography>
                    <Link component={NextLink} href={`/verify/${item.publicId}`} underline="hover">
                      Verificar assinatura publica
                    </Link>
                  </Stack>
                </Card>
              ))}
            </Stack>
          )}
        </Stack>
      </Card>
    </Stack>
  );
}
