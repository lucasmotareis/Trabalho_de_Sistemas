"use client";

import Alert from "@mui/material/Alert";
import Card from "@mui/material/Card";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { VerifySignatureResponse } from "@/types/verification";
import { formatTimestamp } from "@/lib/formatters";

interface VerificationResultCardProps {
  result: VerifySignatureResponse | null;
}

export default function VerificationResultCard({ result }: VerificationResultCardProps) {
  if (!result) {
    return null;
  }

  return (
    <Card variant="outlined" sx={{ mt: 3, p: 3 }}>
      <Stack spacing={2}>
        <Alert severity={result.valid ? "success" : "error"}>
          {result.valid ? "Assinatura valida" : "Assinatura invalida"}
        </Alert>

        <Divider />

        <Stack spacing={1}>
          {result.signatureId ? (
            <Typography variant="body2">
              <strong>ID:</strong> {result.signatureId}
            </Typography>
          ) : null}
          {result.signer ? (
            <Typography variant="body2">
              <strong>Assinante:</strong> {result.signer}
            </Typography>
          ) : null}
          {result.algorithm ? (
            <Typography variant="body2">
              <strong>Algoritmo:</strong> {result.algorithm}
            </Typography>
          ) : null}
          {result.timestamp ? (
            <Typography variant="body2">
              <strong>Data:</strong> {formatTimestamp(result.timestamp)}
            </Typography>
          ) : null}
          {result.message ? (
            <Typography variant="body2" color="text.secondary">
              {result.message}
            </Typography>
          ) : null}
        </Stack>
      </Stack>
    </Card>
  );
}
