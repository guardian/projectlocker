import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import MultistepComponentLoadsOnMount from '../common/LoadsOnMount.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class ProjectTypeComponent extends MultistepComponentLoadsOnMount{
    /* CommonMultistepComponent includes an implementation of ComponentDidUpdate which
    updates the parent with our state
     */
    constructor(props){
        super(props);

        this.endpoint = "/api/projecttype";

        this.state = {
            name: null,
            opensWith: null,
            version: null,
            fileExtension: null,
            plutoType: null,
            plutoSubtype: null,
            loading: false,
            error: null
        }
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
                    <td><input id="pluto-type" className="inputs" value={this.state.plutoType}
                            onChange={event=>this.setState({plutoType: event.target.value})}/></td>
                    <td><a onClick={event=>this.setState({plutoType: null})}>clear</a></td>
                </tr>
                <tr>
                    <td>Pluto subtype identifier, if applicable</td>
                    <td><input id="pluto-subtype" className="inputs" value={this.state.plutoSubtype}
                            onChange={event=>this.setState({plutoSubtype: event.target.value})}/></td>
                    <td><a onClick={event=>this.setState({plutoSubtype: null})}>clear</a></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default ProjectTypeComponent;