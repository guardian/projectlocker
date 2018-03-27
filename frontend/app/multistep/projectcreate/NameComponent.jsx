import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import moment from 'moment';

class NameComponent extends CommonMultistepComponent {
    static propTypes = {
        projectName: PropTypes.string.isRequired,
        fileName: PropTypes.string.isRequired,
        selectionUpdated: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            projectName: "",
            fileName: "",
            autoNameFile: true
        };
        this.projectNameChanged = this.projectNameChanged.bind(this);
        this.fileNameChanged = this.fileNameChanged.bind(this);
    }

    componentWillMount(){
        this.setState({
            projectName: this.props.projectName,
            fileName: this.props.fileName
        });
    }

    makeAutoFilename(title){
        const sanitizer = /[^\w\d_]+/g;
        return moment().format("YYYYMMDD") + "_" + title.replace(sanitizer, "_").toLowerCase();
    }

    projectNameChanged(event){
        const newFileName = this.state.autoNameFile ? this.makeAutoFilename(event.target.value): this.state.fileName;
        this.setState({
            projectName: event.target.value,
            fileName: newFileName
        }, ()=>this.props.selectionUpdated(this.state));
    }

    /* when autoNameFile is set, then the control is disabled so this can't get called */
    fileNameChanged(event){
        this.setState({
            fileName: event.target.value
        }, ()=>this.props.selectionUpdated(this.state));
    }

    render(){
        return <div>
            <h3>Name your project</h3>
            <p>Now, we need a descriptive name for your new project</p>
            <table>
                <tbody>
                <tr>
                    <td>Project Name</td>
                    <td><input id="projectNameInput" onChange={this.projectNameChanged} value={this.state.projectName}/></td>
                </tr>
                <tr>
                    <td>File name</td>
                    <td><input id="fileNameInput" onChange={this.fileNameChanged}
                               value={this.state.fileName} disabled={this.state.autoNameFile}/></td>
                    <td><input id="autoNameCheck" type="checkbox"
                               checked={this.state.autoNameFile} onChange={(event)=>this.setState({autoNameFile: event.target.checked})}/>
                        Automatically name file (recommended)
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default NameComponent;
