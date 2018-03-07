import React from 'react';
import PropTypes from 'prop-types';
import CodeMirror from 'react-codemirror';
import 'codemirror/lib/codemirror.css';
import 'codemirror/mode/python/python.js';

class SourceComponent extends React.Component {
    static propTypes = {
        sourceCode: PropTypes.string.isRequired,
        valueWasSet: PropTypes.func.isRequired
    };

    componentDidUpdate(prevProps,prevState){
        if(prevProps!=this.props && this.props.hasOwnProperty("sourceCode")){
            //for some reason, it appears that the react-codemirror component does not update itself when its props change.
            if(this.editor) this.editor.getCodeMirror().setValue(this.props.sourceCode);
        }
    }

    render(){
        const options = {
            lineNumbers: true,
            readOnly: true,
            mode: {
                name: "python",
                version: 2
            }
        };

        return <div>
            <h3>Review source code</h3>
            <p>Unfortunately this is not editable at the moment</p>
            <CodeMirror ref={editor=>this.editor=editor} disabled={true} className="source-view" value={this.props.sourceCode} options={options}/>
        </div>
    }
}

export default SourceComponent;