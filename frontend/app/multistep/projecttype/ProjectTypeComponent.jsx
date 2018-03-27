import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import MultistepComponentLoadsOnMount from '../common/LoadsOnMount.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import PlutoProjectTypeSelector from '../../Selectors/PlutoProjectTypeSelector.jsx';
import axios from 'axios';

class ProjectTypeComponent extends MultistepComponentLoadsOnMount{
    /* CommonMultistepComponent includes an implementation of ComponentDidUpdate which
    updates the parent with our state
     */
    constructor(props){
        super(props);

        this.endpoint = "/api/projecttype";

        this.state = {
            name: "",
            opensWith: "",
            version: "",
            fileExtension: "",
            plutoType: "",
            plutoSubtype: "",
            loading: false,
            error: null,
            knownPlutoTypes: []
        }
    }

    componentWillMount(){
        axios.get("/api/plutoprojecttypeid").then(result=>{
            this.setState({knownPlutoTypes: result.data.result}, ()=>super.componentWillMount())
        }).catch(error=>console.error(error));
    }

    receivedExistingObject(projectType, cb){
        /* called by the superclass when we get data back for an object */
        console.log("receivedExistingObject");
        this.setState({
            name: projectType.result.name,
            version: projectType.result.targetVersion,
            opensWith: projectType.result.opensWith,
            fileExtension: projectType.result.fileExtension,
            plutoType: projectType.result.plutoType,
            plutoSubtype: projectType.result.plutoSubtype
        }, cb);
    }

    render() {
        if(this.state.error) return <ErrorViewComponent error={this.state.error}/>;

        if(this.state.loading) return <p>Loading...</p>;

        return <div>
            <h3>Project Type</h3>
            <table>
                <tbody>
                <tr>
                    <td>Name of project type</td>
                    <td><input id="project_type_name" className="inputs" value={this.state.name}
                               onChange={event=>this.setState({name: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>Opens with which Mac app?</td>
                    <td><input id="opens_with" className="inputs" value={this.state.opensWith}
                               onChange={event=>this.setState({opensWith: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>Minimum required software version to open it</td>
                    <td><input id="version" className="inputs" value={this.state.version}
                               onChange={event=>this.setState({version: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>File extension for this file type</td>
                    <td><input id="extension" className="inputs" value={this.state.fileExtension}
                               onChange={event=>this.setState({fileExtension: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>Pluto type identifier, if applicable</td>
                    <td><PlutoProjectTypeSelector id="pluto-type"
                                                  plutoProjectTypesList={this.state.knownPlutoTypes}
                                                  selectionUpdated={newValue=>this.setState({plutoType: newValue})}
                                                  selectedType={this.state.plutoType}/></td>
                    <td><a onClick={event=>this.setState({plutoType: ""})}>clear</a></td>
                </tr>
                <tr>
                    <td>Pluto subtype identifier, if applicable</td>
                    <td><PlutoProjectTypeSelector id="pluto-subtype"
                                                  plutoProjectTypesList={this.state.knownPlutoTypes}
                                                  selectionUpdated={newValue=>this.setState({plutoSubtype: newValue})}
                                                  selectedType={this.state.plutoSubType}
                                                  subTypesFor={this.state.plutoType}
                                                  onlyShowSubtypes={true}
                    /></td>
                    <td><a onClick={event=>this.setState({plutoSubtype: ""})}>clear</a></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default ProjectTypeComponent;