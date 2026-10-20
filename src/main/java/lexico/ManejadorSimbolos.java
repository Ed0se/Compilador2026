package lexico;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ManejadorSimbolos {
    private final List<EntidadSimbolo> listaSimbolos;

    public ManejadorSimbolos() {
        this.listaSimbolos = new ArrayList<>();
    }

    public void registrarEntidad(EntidadSimbolo entidad) {
        if (entidad != null) {
            listaSimbolos.add(entidad);
        }
    }

    public boolean verificarExistencia(String id, int nivel) {
        if (id == null) {
            return false;
        }
        for (EntidadSimbolo es : listaSimbolos) {
            if (es.getIdentificador().equals(id) && es.getNivelAmbito() == nivel) {
                return true;
            }
        }
        return false;
    }

    public EntidadSimbolo buscarEnNivel(String id, int nivel) {
        if (id == null) {
            return null;
        }
        for (EntidadSimbolo es : listaSimbolos) {
            if (es.getIdentificador().equals(id) && es.getNivelAmbito() == nivel) {
                return es;
            }
        }
        return null;
    }

    public EntidadSimbolo buscarEnContexto(String id, Stack<Integer> pilaNiveles) {
        if (id == null || pilaNiveles == null || pilaNiveles.isEmpty()) {
            return null;
        }
        for (int i = pilaNiveles.size() - 1; i >= 0; i--) {
            int nivel = pilaNiveles.get(i);
            EntidadSimbolo hallado = buscarEnNivel(id, nivel);
            if (hallado != null) {
                return hallado;
            }
        }
        return null;
    }

    public void limpiar() {
        listaSimbolos.clear();
    }

    public List<EntidadSimbolo> obtenerTodos() {
        return listaSimbolos;
    }
}
