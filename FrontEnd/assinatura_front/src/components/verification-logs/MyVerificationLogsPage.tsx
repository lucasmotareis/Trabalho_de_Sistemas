"use client";

import { useEffect, useMemo, useState } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Typography from "@mui/material/Typography";
import { formatTimestamp } from "@/lib/formatters";
import { verificationLogsService } from "@/services/verification-logs.service";
import type { MyVerificationLogItem } from "@/types/verification-log";

function isUnauthorizedMessage(message: string | null) {
  if (!message) {
    return false;
  }
  const normalized = message.toLowerCase();
  return normalized.includes("forbidden") || normalized.includes("unauthorized");
}

export default function MyVerificationLogsPage() {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [items, setItems] = useState<MyVerificationLogItem[]>([]);

  useEffect(() => {
    let active = true;

    const fetchLogs = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await verificationLogsService.listMyLogs();
        if (active) {
          setItems(response);
        }
      } catch (fetchError) {
        if (active) {
          const message =
            fetchError instanceof Error
              ? fetchError.message
              : "Nao foi possivel carregar seus logs de verificacao.";
          setError(message);
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    fetchLogs();

    return () => {
      active = false;
    };
  }, []);

  const unauthorized = useMemo(() => isUnauthorizedMessage(error), [error]);

  return (
    <Stack spacing={3}>
      <Typography variant="h4" sx={{ fontWeight: 700 }}>
        Meus logs de verificacao
      </Typography>

      {isLoading ? (
        <Stack direction="row" spacing={2} alignItems="center">
          <CircularProgress size={24} />
          <Typography variant="body2">Carregando logs...</Typography>
        </Stack>
      ) : null}

      {error && !unauthorized ? <Alert severity="error">{error}</Alert> : null}
      {unauthorized ? (
        <Alert severity="warning">
          Voce nao tem permissao para acessar este recurso no momento.
        </Alert>
      ) : null}

      {!isLoading && !error && items.length === 0 ? (
        <Card variant="outlined" sx={{ p: 3 }}>
          <Typography variant="body2" color="text.secondary">
            Nenhum log de verificacao encontrado para sua conta.
          </Typography>
        </Card>
      ) : null}

      {!isLoading && !error && items.length > 0 ? (
        <TableContainer component={Card} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Log ID</TableCell>
                <TableCell>Mensagem</TableCell>
                <TableCell>Valido</TableCell>
                <TableCell>Data</TableCell>
                <TableCell>Signature ID</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((item) => (
                <TableRow key={item.logId} hover>
                  <TableCell>{item.logId}</TableCell>
                  <TableCell>{item.message ?? "-"}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={item.valid ? "success" : "error"}
                      label={item.valid ? "Sim" : "Nao"}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>{formatTimestamp(item.verifiedAt)}</TableCell>
                  <TableCell>
                    <Box
                      component="span"
                      sx={{ fontFamily: 'Consolas, "Courier New", monospace', fontSize: 12 }}
                    >
                      {item.signaturePublicId}
                    </Box>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      ) : null}
    </Stack>
  );
}
